package com.vidalvl.app

import com.vidalvl.app.domain.model.Attribute
import com.vidalvl.app.domain.model.Goal
import com.vidalvl.app.domain.model.MissionRules
import com.vidalvl.app.domain.suggestions.SuggestedMission
import com.vidalvl.app.domain.suggestions.SuggestionRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SuggestionRulesTest {
    private fun idea(title: String, description: String? = null) = SuggestedMission(title, description, Attribute.BODY)

    @Test
    fun normalize_ignores_case_accents_and_spaces() {
        assertEquals("leer 30 minutos", SuggestionRules.normalize("  Léer   30 MINUTOS "))
        assertEquals(SuggestionRules.normalize("Planear tu día"), SuggestionRules.normalize("planear tu dia"))
    }

    @Test
    fun monday_of_any_day_of_the_week() {
        val monday = 20731L // 2026-10-05
        assertEquals(monday, SuggestionRules.mondayOf(monday))
        assertEquals(monday, SuggestionRules.mondayOf(monday + 6))
        assertEquals(monday + 7, SuggestionRules.mondayOf(monday + 7))
    }

    @Test
    fun sanitize_drops_blank_duplicate_and_existing_titles() {
        val result = SuggestionRules.sanitize(
            raw = listOf(idea("  "), idea("Caminar"), idea("caminar"), idea("Leer"), idea("Dormir")),
            exclude = setOf("LEER"),
            max = 5
        )
        assertEquals(listOf("Caminar", "Dormir"), result.map { it.title })
    }

    @Test
    fun sanitize_limits_the_count_and_the_text_length() {
        val long = "x".repeat(100)
        val result = SuggestionRules.sanitize(
            raw = listOf(idea(long, long), idea("B"), idea("C"), idea("D")),
            exclude = emptySet(),
            max = 2
        )
        assertEquals(2, result.size)
        assertEquals(MissionRules.MAX_TITLE_LENGTH, result[0].title.length)
        assertEquals(MissionRules.MAX_DESCRIPTION_LENGTH, result[0].description?.length)
    }

    @Test
    fun pools_are_read_one_goal_after_another() {
        val pools = mapOf(
            Goal.FIT to listOf(idea("F1"), idea("F2")),
            Goal.LEARN to listOf(idea("L1"), idea("L2"))
        )
        val picked = SuggestionRules.pickFromPools(pools, setOf(Goal.FIT, Goal.LEARN), 3, 0, emptySet())
        assertEquals(listOf("F1", "L1", "F2"), picked.map { it.title })
    }

    @Test
    fun the_seed_changes_which_ideas_come_first() {
        val pools = mapOf(Goal.FIT to listOf(idea("F1"), idea("F2"), idea("F3")))
        val monday = SuggestionRules.pickFromPools(pools, setOf(Goal.FIT), 1, 0, emptySet()).single().title
        val tuesday = SuggestionRules.pickFromPools(pools, setOf(Goal.FIT), 1, 1, emptySet()).single().title
        assertTrue(monday != tuesday)
    }

    @Test
    fun pools_skip_excluded_titles_and_never_exceed_what_exists() {
        val pools = mapOf(Goal.FIT to listOf(idea("F1"), idea("F2")))
        val picked = SuggestionRules.pickFromPools(pools, setOf(Goal.FIT), 5, 0, setOf("f1"))
        assertEquals(listOf("F2"), picked.map { it.title })
    }

    @Test
    fun goals_without_ideas_give_nothing() {
        assertTrue(SuggestionRules.pickFromPools(emptyMap(), setOf(Goal.FIT), 3, 0, emptySet()).isEmpty())
    }
}
