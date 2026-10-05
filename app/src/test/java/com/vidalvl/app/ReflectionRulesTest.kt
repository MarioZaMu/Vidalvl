package com.vidalvl.app

import com.vidalvl.app.domain.model.Achievement
import com.vidalvl.app.domain.model.AchievementFact
import com.vidalvl.app.domain.model.Attribute
import com.vidalvl.app.domain.model.CompletionFact
import com.vidalvl.app.domain.model.ItemFact
import com.vidalvl.app.domain.model.MissionType
import com.vidalvl.app.domain.model.ReflectionRules
import com.vidalvl.app.domain.model.TimelineEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset

class ReflectionRulesTest {
    private val utc = ZoneOffset.UTC
    private val dayMillis = 86_400_000L
    private val monday = 20731L // 2026-10-05

    private fun fact(
        day: Long,
        hourOffset: Long = 0,
        xp: Int = 8,
        attribute: Attribute = Attribute.MIND,
        type: MissionType = MissionType.DAILY,
        title: String = "Leer"
    ) = CompletionFact(day * dayMillis + hourOffset * 3_600_000L, day, title, attribute, xp, type)

    @Test
    fun an_empty_history_gives_an_empty_timeline() {
        assertTrue(ReflectionRules.timeline(emptyList(), emptyList(), emptyList(), utc).isEmpty())
    }

    @Test
    fun days_come_newest_first_and_events_newest_first() {
        val groups = ReflectionRules.timeline(
            completions = listOf(fact(monday, 8, title = "A"), fact(monday, 9, title = "B"), fact(monday + 1, 7, title = "C")),
            items = emptyList(), achievements = emptyList(), zone = utc
        )
        assertEquals(listOf(monday + 1, monday), groups.map { it.epochDay })
        assertEquals(
            listOf("B", "A"),
            groups[1].events.filterIsInstance<TimelineEvent.MissionDone>().map { it.title }
        )
    }

    @Test
    fun items_and_achievements_join_the_day_they_happened() {
        val groups = ReflectionRules.timeline(
            completions = listOf(fact(monday, 8)),
            items = listOf(ItemFact("hat_011", monday * dayMillis + 8 * 3_600_000L)),
            achievements = listOf(AchievementFact(Achievement.FIRST_STEP, monday * dayMillis + 8 * 3_600_000L)),
            zone = utc
        )
        assertEquals(1, groups.size)
        val kinds = groups.single().events.map { it::class.simpleName }
        // The achievement and the item (consequences) come before the mission that caused them.
        assertEquals(listOf("AchievementUnlocked", "ItemEarned", "MissionDone"), kinds)
    }

    @Test
    fun a_level_up_appears_when_the_xp_crosses_the_threshold() {
        // 100 XP is level 2: thirteen completions of 8 XP cross it (104), the twelfth does not (96).
        val completions = (1..13).map { fact(monday + it, xp = 8) }
        val levelUps = ReflectionRules.timeline(completions, emptyList(), emptyList(), utc)
            .flatMap { it.events }.filterIsInstance<TimelineEvent.LevelUp>()
        assertEquals(1, levelUps.size)
        assertEquals(2, levelUps.single().level)
        assertEquals(completions.last().millis, levelUps.single().millis)
    }

    @Test
    fun the_timeline_is_capped() {
        val completions = (1..50).map { fact(monday + it) }
        val events = ReflectionRules.timeline(completions, emptyList(), emptyList(), utc, limit = 10)
            .sumOf { it.events.size }
        assertEquals(10, events)
    }

    @Test
    fun the_week_starts_on_monday_and_marks_today_and_the_future() {
        val summary = ReflectionRules.weekSummary(listOf(fact(monday), fact(monday + 2)), today = monday + 2)
        assertEquals(7, summary.days.size)
        assertEquals(monday, summary.days.first().epochDay)
        assertEquals(listOf(true, false, true, false, false, false, false), summary.days.map { it.hasActivity })
        assertTrue(summary.days[2].isToday)
        assertFalse(summary.days[1].isFuture)
        assertTrue(summary.days[3].isFuture)
    }

    @Test
    fun only_this_weeks_completions_are_counted_for_the_week() {
        val summary = ReflectionRules.weekSummary(
            listOf(fact(monday - 1), fact(monday), fact(monday + 1), fact(monday + 7)), today = monday + 3
        )
        assertEquals(2, summary.completionsThisWeek)
    }

    @Test
    fun the_best_streak_looks_at_the_whole_history() {
        val past = (0L..3L).map { fact(monday - 30 + it) }
        val summary = ReflectionRules.weekSummary(past + fact(monday), today = monday)
        assertEquals(4, summary.bestStreak)
    }

    @Test
    fun the_most_worked_attribute_wins_and_ties_go_to_the_first_one() {
        val facts = listOf(
            fact(monday, attribute = Attribute.SOCIAL), fact(monday, attribute = Attribute.SOCIAL),
            fact(monday, attribute = Attribute.BODY)
        )
        assertEquals(Attribute.SOCIAL, ReflectionRules.weekSummary(facts, monday).topAttribute)

        val tied = listOf(fact(monday, attribute = Attribute.MIND), fact(monday, attribute = Attribute.BODY))
        assertEquals(Attribute.BODY, ReflectionRules.weekSummary(tied, monday).topAttribute)
        assertNull(ReflectionRules.weekSummary(emptyList(), monday).topAttribute)
    }
}
