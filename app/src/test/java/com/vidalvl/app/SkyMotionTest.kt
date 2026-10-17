package com.vidalvl.app

import com.vidalvl.app.ui.components.cloudX
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SkyMotionTest {
    private val width = 400f
    private val cloud = 50f

    @Test
    fun cycle_end_matches_cycle_start() {
        for (loops in 1..3) {
            val start = cloudX(0f, loops, 0.3f, width, cloud)
            val end = cloudX(1f, loops, 0.3f, width, cloud)
            assertEquals(start, end, 0.001f)
        }
    }

    @Test
    fun cloud_stays_between_off_left_and_off_right() {
        for (i in 0..100) {
            val x = cloudX(i / 100f, 3, 0.7f, width, cloud)
            assertTrue(x >= -cloud && x <= width)
        }
    }

    @Test
    fun cloud_only_moves_forward_within_a_trip() {
        val a = cloudX(0.10f, 1, 0f, width, cloud)
        val b = cloudX(0.20f, 1, 0f, width, cloud)
        assertTrue(b > a)
    }
}
