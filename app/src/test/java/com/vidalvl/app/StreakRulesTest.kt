package com.vidalvl.app

import com.vidalvl.app.domain.model.StreakRules
import org.junit.Assert.assertEquals
import org.junit.Test

class StreakRulesTest {
    private val today = 100L

    @Test
    fun no_completions_means_no_streak() {
        assertEquals(0, StreakRules.currentStreak(emptySet(), today))
    }

    @Test
    fun counts_consecutive_days_including_today() {
        assertEquals(3, StreakRules.currentStreak(setOf(98L, 99L, 100L), today))
    }

    @Test
    fun streak_stays_alive_while_today_is_still_open() {
        assertEquals(2, StreakRules.currentStreak(setOf(98L, 99L), today))
    }

    @Test
    fun a_missed_day_restarts_the_count() {
        assertEquals(0, StreakRules.currentStreak(setOf(96L, 97L, 98L), today))
        assertEquals(1, StreakRules.currentStreak(setOf(97L, 98L, 100L), today))
    }

    @Test
    fun several_completions_on_one_day_count_once() {
        // The input is a set of days, so duplicates are impossible by construction.
        assertEquals(1, StreakRules.currentStreak(setOf(100L), today))
    }
}
