package com.vidalvl.app.data.local

import com.vidalvl.app.domain.model.Attribute
import com.vidalvl.app.domain.model.GameRules
import com.vidalvl.app.domain.model.GoalRules
import com.vidalvl.app.domain.model.MissionType
import com.vidalvl.app.domain.model.StreakRules
import com.vidalvl.app.domain.suggestions.AttributeSnapshot
import com.vidalvl.app.domain.suggestions.SuggestedMission
import com.vidalvl.app.domain.suggestions.SuggestionContext
import com.vidalvl.app.domain.suggestions.SuggestionStore
import java.time.LocalDate

private const val RECENT_DAYS = 7L

/** Reads what the AI is told about the player and stores the suggested missions, all in Room. */
class RoomSuggestionStore(private val db: VidalvlDatabase) : SuggestionStore {
    override suspend fun context(): SuggestionContext {
        val today = LocalDate.now().toEpochDay()
        val saved = db.goalsDao().get()
        val states = db.attributeDao().getAll()
        return SuggestionContext(
            goals = saved?.let { GoalRules.decode(it.goals) } ?: emptySet(),
            dailyMinutes = saved?.dailyMinutes ?: GoalRules.DEFAULT_MINUTES,
            attributes = Attribute.entries.map { attribute ->
                val state = states.firstOrNull { it.attribute == attribute } ?: AttributeStateEntity(attribute)
                AttributeSnapshot(
                    attribute = attribute,
                    level = GameRules.levelFor(state.xp).level,
                    glow = GameRules.glowFor(state.lastCompletedDayEpoch, today)
                )
            },
            streak = StreakRules.currentStreak(db.completionDao().allDays().toSet(), today),
            existingTitles = db.missionDao().activeTitles(),
            recentTitles = db.completionDao().titlesSince(today - RECENT_DAYS)
        )
    }

    override suspend fun expire(today: Long, mondayOfWeek: Long) =
        db.missionDao().expireSuggestions(today, mondayOfWeek)

    override suspend fun insert(missions: List<SuggestedMission>, type: MissionType, generatedFor: Long) {
        db.missionDao().insertAll(
            missions.map {
                MissionEntity(
                    title = it.title,
                    description = it.description,
                    attribute = it.attribute,
                    type = type,
                    xpReward = GameRules.missionXp(type),
                    generatedFor = generatedFor
                )
            }
        )
    }
}
