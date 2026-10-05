package com.vidalvl.app.data.local

import android.content.Context
import androidx.annotation.ArrayRes
import com.vidalvl.app.R
import com.vidalvl.app.domain.model.Goal
import com.vidalvl.app.domain.model.MissionType
import com.vidalvl.app.domain.suggestions.FallbackSuggestions
import com.vidalvl.app.domain.suggestions.SuggestedMission
import com.vidalvl.app.domain.suggestions.SuggestionRules

/** Built-in ideas from string arrays, so they follow the app language. */
class ResourceFallbackSuggestions(context: Context) : FallbackSuggestions {
    private val resources = context.applicationContext.resources

    override fun pick(
        goals: Set<Goal>,
        type: MissionType,
        count: Int,
        seed: Long,
        exclude: Set<String>
    ): List<SuggestedMission> {
        val pools = goals.associateWith { goal ->
            resources.getStringArray(arrayFor(goal, type)).map { SuggestedMission(it, null, goal.attribute) }
        }
        return SuggestionRules.pickFromPools(pools, goals, count, seed, exclude)
    }

    @ArrayRes
    private fun arrayFor(goal: Goal, type: MissionType): Int = when (type) {
        MissionType.DAILY -> when (goal) {
            Goal.FIT -> R.array.fallback_daily_fit
            Goal.LEARN -> R.array.fallback_daily_learn
            Goal.DISCIPLINE -> R.array.fallback_daily_discipline
            Goal.RELATIONSHIPS -> R.array.fallback_daily_relationships
            Goal.SLEEP -> R.array.fallback_daily_sleep
            Goal.LESS_PHONE -> R.array.fallback_daily_less_phone
            Goal.EAT_WELL -> R.array.fallback_daily_eat_well
            Goal.STRESS -> R.array.fallback_daily_stress
        }

        MissionType.WEEKLY -> when (goal) {
            Goal.FIT -> R.array.fallback_weekly_fit
            Goal.LEARN -> R.array.fallback_weekly_learn
            Goal.DISCIPLINE -> R.array.fallback_weekly_discipline
            Goal.RELATIONSHIPS -> R.array.fallback_weekly_relationships
            Goal.SLEEP -> R.array.fallback_weekly_sleep
            Goal.LESS_PHONE -> R.array.fallback_weekly_less_phone
            Goal.EAT_WELL -> R.array.fallback_weekly_eat_well
            Goal.STRESS -> R.array.fallback_weekly_stress
        }
    }
}
