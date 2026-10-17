package com.vidalvl.app

import com.vidalvl.app.ui.components.pixelCornerPoints
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PixelShapeTest {
    @Test
    fun point_count_is_one_start_plus_each_corner() {
        // Each corner has 1 + 2 * steps points.
        assertEquals(1 + 4 * (1 + 2 * 2), pixelCornerPoints(100f, 60f, 3f, 2).size)
        assertEquals(1 + 4 * (1 + 2 * 1), pixelCornerPoints(100f, 60f, 3f, 1).size)
    }

    @Test
    fun top_right_corner_is_a_two_step_staircase() {
        val p = pixelCornerPoints(100f, 60f, 3f, 2)
        assertEquals(6f, p[0].x, 0f)
        assertEquals(94f, p[1].x, 0f) // top row cuts 2 units
        assertEquals(0f, p[1].y, 0f)
        assertEquals(94f, p[2].x, 0f)
        assertEquals(3f, p[2].y, 0f)
        assertEquals(97f, p[3].x, 0f) // next row cuts 1 unit
        assertEquals(3f, p[3].y, 0f)
        assertEquals(97f, p[4].x, 0f)
        assertEquals(6f, p[4].y, 0f)
        assertEquals(100f, p[5].x, 0f)
        assertEquals(6f, p[5].y, 0f)
    }

    @Test
    fun all_points_stay_inside_the_box() {
        val p = pixelCornerPoints(120f, 48f, 4f, 2)
        assertTrue(p.all { it.x in 0f..120f && it.y in 0f..48f })
    }

    @Test
    fun unit_shrinks_on_tiny_boxes() {
        val p = pixelCornerPoints(10f, 10f, 8f, 2)
        assertTrue(p.all { it.x in 0f..10f && it.y in 0f..10f })
    }
}
