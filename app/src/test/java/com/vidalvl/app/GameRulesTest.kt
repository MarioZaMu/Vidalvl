package com.vidalvl.app

import com.vidalvl.app.domain.model.GameRules
import org.junit.Assert.assertEquals
import org.junit.Test

class GameRulesTest {
    @Test
    fun levels_follow_xp_thresholds() {
        assertEquals(1, GameRules.levelFor(99).level)
        assertEquals(2, GameRules.levelFor(100).level)
        assertEquals(0, GameRules.levelFor(100).xpIntoLevel)
        assertEquals(2, GameRules.levelFor(219).level)
        assertEquals(3, GameRules.levelFor(220).level)
    }

    @Test
    fun glow_dims_slowly_and_never_below_minimum() {
        val today = 100L
        assertEquals(1f, GameRules.glowFor(today, today), 0.001f)
        assertEquals(1f, GameRules.glowFor(today - 1, today), 0.001f)
        assertEquals(0.9f, GameRules.glowFor(today - 2, today), 0.001f)
        assertEquals(0.3f, GameRules.glowFor(today - 8, today), 0.001f)
        assertEquals(0.3f, GameRules.glowFor(today - 60, today), 0.001f)
        assertEquals(0.6f, GameRules.glowFor(null, today), 0.001f)
    }
}