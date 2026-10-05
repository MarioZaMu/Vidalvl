package com.vidalvl.app

import com.vidalvl.app.domain.model.GameRules
import com.vidalvl.app.domain.model.MissionRules
import com.vidalvl.app.domain.model.MissionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MissionRulesTest {
    @Test
    fun blank_titles_are_invalid() {
        assertFalse(MissionRules.isValidTitle(""))
        assertFalse(MissionRules.isValidTitle("   "))
    }

    @Test
    fun title_is_trimmed_and_limited() {
        assertTrue(MissionRules.isValidTitle("  Leer  "))
        assertEquals("Leer", MissionRules.cleanTitle("  Leer  "))
        assertFalse(MissionRules.isValidTitle("x".repeat(MissionRules.MAX_TITLE_LENGTH + 1)))
        assertEquals(MissionRules.MAX_TITLE_LENGTH, MissionRules.cleanTitle("x".repeat(100)).length)
    }

    @Test
    fun blank_description_becomes_null() {
        assertNull(MissionRules.cleanDescription("   "))
        assertEquals("Con calma", MissionRules.cleanDescription(" Con calma "))
    }

    @Test
    fun xp_is_fixed_by_mission_type() {
        assertEquals(8, GameRules.missionXp(MissionType.DAILY))
        assertEquals(15, GameRules.missionXp(MissionType.WEEKLY))
    }
}