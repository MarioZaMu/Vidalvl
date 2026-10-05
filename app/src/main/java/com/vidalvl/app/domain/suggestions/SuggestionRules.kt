package com.vidalvl.app.domain.suggestions

import com.vidalvl.app.domain.model.Goal
import com.vidalvl.app.domain.model.MissionRules
import java.text.Normalizer
import java.time.LocalDate

object SuggestionRules {
    const val DAILY_COUNT = 3
    const val WEEKLY_COUNT = 2

    /** Weeks start on Monday, as everywhere else in the app. */
    fun mondayOf(epochDay: Long): Long =
        epochDay - (LocalDate.ofEpochDay(epochDay).dayOfWeek.value - 1)

    /** Lowercase, no accents, single spaces: "  Leer  30 Minutos " and "leer 30 minutos" are the same title. */
    fun normalize(title: String): String =
        Normalizer.normalize(title, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .lowercase()
            .replace(Regex("\\s+"), " ")
            .trim()

    /**
     * Cleans AI output: trims and limits text, drops blank titles and anything already in [exclude]
     * (or repeated inside the list), and keeps at most [max].
     */
    fun sanitize(
        raw: List<SuggestedMission>,
        exclude: Set<String>,
        max: Int
    ): List<SuggestedMission> {
        val seen = exclude.map(::normalize).toMutableSet()
        val result = mutableListOf<SuggestedMission>()
        for (item in raw) {
            val title = MissionRules.cleanTitle(item.title)
            if (title.isEmpty()) continue
            if (!seen.add(normalize(title))) continue
            result += item.copy(title = title, description = item.description?.let(MissionRules::cleanDescription))
            if (result.size == max) break
        }
        return result
    }

    /**
     * Picks [count] ideas from per-goal pools, taking one goal after another so every chosen goal is represented.
     * [seed] rotates the starting point (the day number), so the same pool gives different ideas on different days.
     */
    fun pickFromPools(
        pools: Map<Goal, List<SuggestedMission>>,
        goals: Set<Goal>,
        count: Int,
        seed: Long,
        exclude: Set<String>
    ): List<SuggestedMission> {
        val usable = goals.filter { !pools[it].isNullOrEmpty() }
        if (usable.isEmpty()) return emptyList()
        val seen = exclude.map(::normalize).toMutableSet()
        val result = mutableListOf<SuggestedMission>()
        val longest = usable.maxOf { pools.getValue(it).size }
        var round = 0
        while (result.size < count && round < longest) {
            for (goal in usable) {
                val pool = pools.getValue(goal)
                val index = Math.floorMod(seed + round, pool.size.toLong()).toInt()
                val item = pool[index]
                if (seen.add(normalize(item.title))) result += item
                if (result.size == count) break
            }
            round++
        }
        return result
    }
}
