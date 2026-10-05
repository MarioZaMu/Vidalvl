package com.vidalvl.app

import com.vidalvl.app.domain.model.Achievement
import com.vidalvl.app.domain.model.AchievementRules
import com.vidalvl.app.domain.model.AchievementStats
import com.vidalvl.app.domain.model.Attribute
import com.vidalvl.app.domain.model.ItemCatalog
import com.vidalvl.app.domain.model.ItemUnlock
import com.vidalvl.app.domain.model.StreakRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AchievementRulesTest {
    private fun stats(
        completions: Int = 0,
        weekly: Int = 0,
        bestStreak: Int = 0,
        totalLevel: Int = 1,
        levels: Map<Attribute, Int> = Attribute.entries.associateWith { 1 },
        items: Int = 0,
        perfectDay: Boolean = false,
        returned: Boolean = false
    ) = AchievementStats(completions, weekly, bestStreak, totalLevel, levels, items, perfectDay, returned)

    private fun earned(stats: AchievementStats) =
        AchievementRules.newlyEarned(stats, emptySet()).toSet()

    @Test
    fun a_new_player_has_earned_nothing() {
        assertTrue(earned(stats()).isEmpty())
    }

    @Test
    fun the_first_mission_and_the_first_weekly() {
        assertEquals(setOf(Achievement.FIRST_STEP), earned(stats(completions = 1)))
        assertTrue(Achievement.FIRST_WEEKLY in earned(stats(completions = 3, weekly = 1)))
    }

    @Test
    fun streak_achievements_use_the_best_streak_ever() {
        val result = earned(stats(completions = 40, bestStreak = 14))
        assertTrue(Achievement.STREAK_3 in result)
        assertTrue(Achievement.STREAK_7 in result)
        assertTrue(Achievement.STREAK_14 in result)
        assertFalse(Achievement.STREAK_30 in result)
    }

    @Test
    fun counts_and_levels() {
        assertTrue(Achievement.TOTAL_25 in earned(stats(completions = 25)))
        assertFalse(Achievement.TOTAL_25 in earned(stats(completions = 24)))
        assertTrue(Achievement.LEVEL_5 in earned(stats(totalLevel = 5)))
        assertFalse(Achievement.LEVEL_10 in earned(stats(totalLevel = 9)))
    }

    @Test
    fun balance_needs_all_four_attributes_and_specialist_needs_one() {
        val three = Attribute.entries.associateWith { if (it == Attribute.SOCIAL) 2 else 3 }
        assertFalse(Achievement.BALANCE in earned(stats(levels = three)))
        assertTrue(Achievement.BALANCE in earned(stats(levels = Attribute.entries.associateWith { 3 })))

        val one = Attribute.entries.associateWith { if (it == Attribute.MIND) 5 else 1 }
        assertTrue(Achievement.SPECIALIST in earned(stats(levels = one)))
    }

    @Test
    fun moments_only_count_when_they_just_happened() {
        assertTrue(Achievement.PERFECT_DAY in earned(stats(perfectDay = true)))
        assertFalse(Achievement.PERFECT_DAY in earned(stats()))
        assertTrue(Achievement.COME_BACK in earned(stats(returned = true)))
        assertNull(AchievementRules.progress(Achievement.PERFECT_DAY, stats()))
    }

    @Test
    fun already_unlocked_achievements_are_not_reported_again() {
        val result = AchievementRules.newlyEarned(stats(completions = 1), setOf(Achievement.FIRST_STEP))
        assertTrue(result.isEmpty())
    }

    @Test
    fun progress_shows_how_far_along_it_is() {
        val progress = AchievementRules.progress(Achievement.TOTAL_25, stats(completions = 10))!!
        assertEquals(10, progress.current)
        assertEquals(25, progress.target)
        assertEquals(0.4f, progress.fraction, 0.001f)
        assertEquals(1f, AchievementRules.progress(Achievement.TOTAL_25, stats(completions = 99))!!.fraction, 0.001f)
    }

    @Test
    fun every_special_item_is_the_prize_of_exactly_one_achievement() {
        val special = ItemCatalog.items.filter { it.unlock is ItemUnlock.RewardMission }.map { it.id }.toSet()
        val prizes = Achievement.entries.mapNotNull { it.rewardItemId }
        assertEquals(special, prizes.toSet())
        assertEquals(prizes.size, prizes.toSet().size)
    }

    @Test
    fun the_item_to_achievement_lookup_works() {
        assertEquals(Achievement.STREAK_7, AchievementRules.givingItem("hat_013"))
        assertNull(AchievementRules.givingItem("hat_011"))
    }

    @Test
    fun longest_streak_finds_the_best_run_anywhere() {
        assertEquals(0, StreakRules.longestStreak(emptySet()))
        assertEquals(1, StreakRules.longestStreak(setOf(5L)))
        assertEquals(4, StreakRules.longestStreak(setOf(1L, 2L, 10L, 11L, 12L, 13L, 20L)))
    }
}
