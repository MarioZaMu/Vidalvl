package com.vidalvl.app

import com.vidalvl.app.domain.model.AvatarConfig
import org.junit.Assert.assertTrue
import org.junit.Test

/** Every hairstyle the creator offers must have its front sprites, or opening the creator would crash. */
class HairSpritesTest {
    private fun hasDrawable(name: String) =
        R.drawable::class.java.fields.any { it.name == name }

    @Test
    fun every_hairstyle_has_front_and_trimmed_front_sprites() {
        val missing = (1..AvatarConfig.HAIR_STYLES).flatMap { n ->
            listOf("hair_${n}_front", "hair_${n}_hat_front").filterNot(::hasDrawable)
        }
        assertTrue("Missing sprites: $missing", missing.isEmpty())
    }

    @Test
    fun hair_styles_count_matches_the_sprites_shipped() {
        // One more than the count must not exist, so a new sprite pack forces updating HAIR_STYLES.
        assertTrue(!hasDrawable("hair_${AvatarConfig.HAIR_STYLES + 1}_front"))
    }
}
