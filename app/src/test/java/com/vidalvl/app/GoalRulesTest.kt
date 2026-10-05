package com.vidalvl.app

import com.vidalvl.app.domain.model.Goal
import com.vidalvl.app.domain.model.GoalRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalRulesTest {
    @Test
    fun toggle_adds_and_removes() {
        val one = GoalRules.toggle(emptySet(), Goal.FIT)
        assertEquals(setOf(Goal.FIT), one)
        assertEquals(emptySet<Goal>(), GoalRules.toggle(one, Goal.FIT))
    }

    @Test
    fun a_fourth_goal_is_ignored() {
        val three = setOf(Goal.FIT, Goal.LEARN, Goal.SLEEP)
        assertEquals(three, GoalRules.toggle(three, Goal.STRESS))
    }

    @Test
    fun a_goal_can_still_be_removed_when_full() {
        val three = setOf(Goal.FIT, Goal.LEARN, Goal.SLEEP)
        assertEquals(setOf(Goal.FIT, Goal.LEARN), GoalRules.toggle(three, Goal.SLEEP))
    }

    @Test
    fun at_least_one_goal_is_needed_to_continue() {
        assertFalse(GoalRules.canContinue(emptySet()))
        assertTrue(GoalRules.canContinue(setOf(Goal.FIT)))
    }

    @Test
    fun goals_survive_encoding_in_order() {
        val goals = linkedSetOf(Goal.LEARN, Goal.FIT, Goal.STRESS)
        assertEquals(goals.toList(), GoalRules.decode(GoalRules.encode(goals)).toList())
    }

    @Test
    fun unknown_or_blank_names_are_dropped() {
        assertEquals(setOf(Goal.FIT), GoalRules.decode("FIT, FUTURE_GOAL,,"))
        assertEquals(emptySet<Goal>(), GoalRules.decode(""))
    }

    @Test
    fun default_time_is_one_of_the_options() {
        assertTrue(GoalRules.DEFAULT_MINUTES in GoalRules.MINUTE_OPTIONS)
    }
}
