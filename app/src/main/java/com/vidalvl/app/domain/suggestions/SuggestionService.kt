package com.vidalvl.app.domain.suggestions

import com.vidalvl.app.domain.model.MissionType
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate

enum class SuggestionOutcome {
    /** Today's (and this week's) missions already exist. */
    NOTHING_TO_DO,

    /** The user has not yet chosen between AI and built-in ideas. */
    NEEDS_CONSENT,
    GENERATED_WITH_AI,
    GENERATED_LOCALLY
}

/**
 * Keeps the day's and the week's suggested missions up to date. At most once per day for the daily ones and
 * once per week for the weekly ones, even if the user deletes them. The default and the user's own missions
 * are never touched. Whatever the AI does not deliver is filled from the built-in ideas.
 */
class SuggestionService(
    private val store: SuggestionStore,
    private val suggester: MissionSuggester,
    private val fallback: FallbackSuggestions,
    private val settings: SuggestionSettings,
    private val language: () -> String,
    private val today: () -> Long
) {
    private val lock = Mutex()

    suspend fun ensureCurrent(): SuggestionOutcome = lock.withLock {
        val day = today()
        val monday = SuggestionRules.mondayOf(day)
        store.expire(day, monday)

        val needDaily = settings.lastDailyDay != day
        val needWeekly = settings.lastWeeklyMonday != monday
        if (!needDaily && !needWeekly) return SuggestionOutcome.NOTHING_TO_DO
        val aiEnabled = settings.aiEnabled ?: return SuggestionOutcome.NEEDS_CONSENT

        val context = store.context()
        val taken = (context.existingTitles + context.recentTitles).toMutableSet()

        val aiBatch = if (aiEnabled) {
            val answer = suggester.suggest(
                context = context,
                dailyCount = if (needDaily) SuggestionRules.DAILY_COUNT else 0,
                weeklyCount = if (needWeekly) SuggestionRules.WEEKLY_COUNT else 0,
                language = language()
            )
            (answer as? SuggestionResult.Success)?.batch
        } else null

        var usedAi = false

        fun build(type: MissionType, fromAi: List<SuggestedMission>, count: Int, seed: Long): List<SuggestedMission> {
            val clean = SuggestionRules.sanitize(fromAi, taken, count)
            taken += clean.map { it.title }
            if (clean.isNotEmpty()) usedAi = true
            val missing = count - clean.size
            val topUp = if (missing > 0) fallback.pick(context.goals, type, missing, seed, taken) else emptyList()
            taken += topUp.map { it.title }
            return clean + topUp
        }

        if (needDaily) {
            val missions = build(MissionType.DAILY, aiBatch?.daily.orEmpty(), SuggestionRules.DAILY_COUNT, day)
            store.insert(missions, MissionType.DAILY, day)
            settings.lastDailyDay = day
        }
        if (needWeekly) {
            val missions = build(MissionType.WEEKLY, aiBatch?.weekly.orEmpty(), SuggestionRules.WEEKLY_COUNT, monday)
            store.insert(missions, MissionType.WEEKLY, monday)
            settings.lastWeeklyMonday = monday
        }
        if (usedAi) SuggestionOutcome.GENERATED_WITH_AI else SuggestionOutcome.GENERATED_LOCALLY
    }
}
