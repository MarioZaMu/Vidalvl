package com.vidalvl.app

import com.vidalvl.app.domain.model.PhotoRules
import org.junit.Assert.assertEquals
import org.junit.Test

class PhotoRulesTest {
    @Test
    fun small_photos_are_not_scaled_up() {
        assertEquals(640 to 480, PhotoRules.targetSize(640, 480))
    }

    @Test
    fun landscape_photo_is_limited_by_its_width() {
        assertEquals(1024 to 768, PhotoRules.targetSize(4000, 3000))
    }

    @Test
    fun portrait_photo_is_limited_by_its_height() {
        assertEquals(768 to 1024, PhotoRules.targetSize(3000, 4000))
    }

    @Test
    fun sides_never_collapse_to_zero() {
        val (w, h) = PhotoRules.targetSize(100_000, 10)
        assertEquals(1024, w)
        assertEquals(1, h)
    }
}
