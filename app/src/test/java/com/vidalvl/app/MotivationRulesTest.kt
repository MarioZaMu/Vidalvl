package com.vidalvl.app

import com.vidalvl.app.domain.model.Attribute
import com.vidalvl.app.domain.model.Goal
import com.vidalvl.app.domain.model.MotivationInput
import com.vidalvl.app.domain.model.MotivationKind
import com.vidalvl.app.domain.model.MotivationRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MotivationRulesTest {
    private fun input(
        done: Int = 0,
        total: Int = 5,
        streak: Int = 0,
        daysSinceLast: Int? = 1,
        goals: Set<Goal> = setOf(Goal.FIT),
        neglected: List<Attribute> = emptyList(),
        seed: Long = 0
    ) = MotivationInput(done, total, streak, daysSinceLast, goals, neglected, seed)

    private fun kind(input: MotivationInput) = MotivationRules.pick(input).kind

    @Test
    fun finishing_every_daily_mission_is_celebrated() {
        assertEquals(MotivationKind.ALL_DONE, kind(input(done = 5, total = 5)))
    }

    @Test
    fun an_empty_day_is_not_a_finished_day() {
        assertEquals(MotivationKind.START, kind(input(done = 0, total = 0)))
    }

    @Test
    fun coming_back_after_days_away_gets_a_welcome_not_a_reproach() {
        assertEquals(MotivationKind.WELCOME_BACK, kind(input(daysSinceLast = 3)))
        assertEquals(MotivationKind.WELCOME_BACK, kind(input(daysSinceLast = 20, streak = 0)))
    }

    @Test
    fun a_short_break_is_not_treated_as_an_absence() {
        assertEquals(MotivationKind.START, kind(input(daysSinceLast = 2)))
    }

    @Test
    fun a_new_player_gets_a_normal_start() {
        assertEquals(MotivationKind.START, kind(input(daysSinceLast = null)))
    }

    @Test
    fun streak_milestones_are_celebrated_once_something_is_done_today() {
        val message = MotivationRules.pick(input(done = 1, streak = 7))
        assertEquals(MotivationKind.STREAK, message.kind)
        assertEquals(7, message.first)
        assertEquals(MotivationKind.PROGRESS, kind(input(done = 1, streak = 6)))
    }

    @Test
    fun a_streak_milestone_alone_does_not_interrupt_a_fresh_day() {
        assertEquals(MotivationKind.START, kind(input(done = 0, streak = 7)))
    }

    @Test
    fun partial_progress_reports_the_numbers() {
        val message = MotivationRules.pick(input(done = 2, total = 5))
        assertEquals(MotivationKind.PROGRESS, message.kind)
        assertEquals(2, message.first)
        assertEquals(5, message.second)
    }

    @Test
    fun a_neglected_attribute_is_mentioned_at_the_start_of_the_day() {
        val message = MotivationRules.pick(input(neglected = listOf(Attribute.MIND, Attribute.BODY)))
        assertEquals(MotivationKind.NEGLECTED, message.kind)
        assertEquals(Attribute.MIND, message.attribute)
    }

    @Test
    fun the_start_message_follows_one_of_the_players_goals() {
        val goals = linkedSetOf(Goal.FIT, Goal.LEARN, Goal.SLEEP)
        val seen = (0L..5L).map { MotivationRules.pick(input(goals = goals, seed = it)).goal }.toSet()
        assertEquals(goals, seen)
    }

    @Test
    fun without_goals_the_message_is_generic() {
        assertEquals(MotivationKind.GENERIC, kind(input(goals = emptySet())))
    }

    @Test
    fun neglected_attributes_are_listed_most_neglected_first() {
        val glows = mapOf(Attribute.BODY to 0.3f, Attribute.MIND to 0.5f, Attribute.SOCIAL to 1.0f)
        assertEquals(listOf(Attribute.BODY, Attribute.MIND), MotivationRules.neglected(glows, emptySet()))
    }

    @Test
    fun an_untrained_attribute_is_not_neglected() {
        // 0.6 is the neutral glow of an attribute that was never practiced.
        assertTrue(MotivationRules.neglected(mapOf(Attribute.BODY to 0.6f), emptySet()).isEmpty())
    }

    @Test
    fun only_attributes_linked_to_the_goals_are_mentioned() {
        val glows = mapOf(Attribute.BODY to 0.3f, Attribute.MIND to 0.4f)
        // LEARN leans on MIND.
        assertEquals(listOf(Attribute.MIND), MotivationRules.neglected(glows, setOf(Goal.LEARN)))
    }

    @Test
    fun the_variant_is_never_negative() {
        assertTrue(MotivationRules.pick(input(seed = -7)).variant >= 0)
        assertTrue(MotivationRules.pick(input(seed = 20_000)).variant >= 0)
    }
}
