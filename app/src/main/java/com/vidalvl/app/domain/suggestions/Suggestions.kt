package com.vidalvl.app.domain.suggestions

import com.vidalvl.app.domain.model.Attribute
import com.vidalvl.app.domain.model.Goal
import com.vidalvl.app.domain.model.MissionType
import com.vidalvl.app.domain.verification.FailureKind

/** A mission idea, from the AI or from the built-in list. XP is not part of it: the type sets it. */
data class SuggestedMission(
    val title: String,
    val description: String?,
    val attribute: Attribute
)

data class AttributeSnapshot(val attribute: Attribute, val level: Int, val glow: Float)

/** What the AI is told about the player. Titles are the user's own text; photos are never included. */
data class SuggestionContext(
    val goals: Set<Goal>,
    val dailyMinutes: Int,
    val attributes: List<AttributeSnapshot>,
    val streak: Int,
    /** Active missions, so ideas do not repeat them. */
    val existingTitles: List<String>,
    /** Missions completed in the last days, for variety. */
    val recentTitles: List<String>
)

data class SuggestionBatch(
    val daily: List<SuggestedMission>,
    val weekly: List<SuggestedMission>
)

sealed interface SuggestionResult {
    data class Success(val batch: SuggestionBatch) : SuggestionResult
    data class Failed(val kind: FailureKind) : SuggestionResult
}

interface MissionSuggester {
    suspend fun suggest(
        context: SuggestionContext,
        dailyCount: Int,
        weeklyCount: Int,
        language: String
    ): SuggestionResult
}

/** Offline ideas, used when the AI is off, unreachable or returns too little. */
interface FallbackSuggestions {
    fun pick(
        goals: Set<Goal>,
        type: MissionType,
        count: Int,
        seed: Long,
        exclude: Set<String>
    ): List<SuggestedMission>
}

/** Where suggested missions are read from and written to. */
interface SuggestionStore {
    suspend fun context(): SuggestionContext

    /** Hides suggested missions whose day (or week) has passed. History and XP stay. */
    suspend fun expire(today: Long, mondayOfWeek: Long)

    suspend fun insert(missions: List<SuggestedMission>, type: MissionType, generatedFor: Long)
}

/** Settings the service needs. [aiEnabled] is null until the user has been asked. */
interface SuggestionSettings {
    var aiEnabled: Boolean?
    var lastDailyDay: Long
    var lastWeeklyMonday: Long
}
