package com.vidalvl.app.ui.avatar

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.vidalvl.app.R
import com.vidalvl.app.domain.model.AvatarConfig
import com.vidalvl.app.domain.model.AvatarLayer
import com.vidalvl.app.domain.model.CatalogItem
import com.vidalvl.app.domain.model.Gender
import com.vidalvl.app.domain.model.HairEffect
import com.vidalvl.app.domain.model.ItemSlot

private const val SPRITE_SIZE = 32

// First sprite row of the legs; everything above it moves when breathing.
private const val LEGS_ROW = 27

// Square region (head and shoulders) shown by thumbnails so hair, eyes and hats are readable.
private const val CROP_X = 4
private const val CROP_Y = 0
private const val CROP_SIZE = 24

private val MALE_BODIES = intArrayOf(
    R.drawable.body_male_1, R.drawable.body_male_2, R.drawable.body_male_3,
    R.drawable.body_male_4, R.drawable.body_male_5
)
private val FEMALE_BODIES = intArrayOf(
    R.drawable.body_female_1, R.drawable.body_female_2, R.drawable.body_female_3,
    R.drawable.body_female_4, R.drawable.body_female_5
)

private val MALE_EYES = intArrayOf(
    R.drawable.eyes_male_01, R.drawable.eyes_male_02, R.drawable.eyes_male_03, R.drawable.eyes_male_04,
    R.drawable.eyes_male_05, R.drawable.eyes_male_06, R.drawable.eyes_male_07, R.drawable.eyes_male_08,
    R.drawable.eyes_male_09, R.drawable.eyes_male_10
)
private val FEMALE_EYES = intArrayOf(
    R.drawable.eyes_female_01, R.drawable.eyes_female_02, R.drawable.eyes_female_03, R.drawable.eyes_female_04,
    R.drawable.eyes_female_05, R.drawable.eyes_female_06, R.drawable.eyes_female_07, R.drawable.eyes_female_08,
    R.drawable.eyes_female_09, R.drawable.eyes_female_10
)

private val HAIR_FRONT = intArrayOf(
    R.drawable.hair_1_front, R.drawable.hair_2_front, R.drawable.hair_3_front, R.drawable.hair_4_front,
    R.drawable.hair_5_front, R.drawable.hair_6_front, R.drawable.hair_7_front, R.drawable.hair_8_front,
    R.drawable.hair_9_front, R.drawable.hair_10_front, R.drawable.hair_11_front,
    R.drawable.hair_12_front, R.drawable.hair_13_front, R.drawable.hair_14_front,
    R.drawable.hair_15_front, R.drawable.hair_16_front, R.drawable.hair_17_front,
    R.drawable.hair_18_front, R.drawable.hair_19_front, R.drawable.hair_20_front,
    R.drawable.hair_21_front, R.drawable.hair_22_front, R.drawable.hair_23_front,
    R.drawable.hair_24_front, R.drawable.hair_25_front, R.drawable.hair_26_front,
    R.drawable.hair_27_front, R.drawable.hair_28_front, R.drawable.hair_29_front,
    R.drawable.hair_30_front, R.drawable.hair_31_front, R.drawable.hair_32_front,
    R.drawable.hair_33_front
)
private val HAIR_FRONT_TRIMMED = intArrayOf(
    R.drawable.hair_1_hat_front, R.drawable.hair_2_hat_front, R.drawable.hair_3_hat_front,
    R.drawable.hair_4_hat_front, R.drawable.hair_5_hat_front, R.drawable.hair_6_hat_front,
    R.drawable.hair_7_hat_front, R.drawable.hair_8_hat_front, R.drawable.hair_9_hat_front,
    R.drawable.hair_10_hat_front, R.drawable.hair_11_hat_front, R.drawable.hair_12_hat_front,
    R.drawable.hair_13_hat_front, R.drawable.hair_14_hat_front, R.drawable.hair_15_hat_front,
    R.drawable.hair_16_hat_front, R.drawable.hair_17_hat_front, R.drawable.hair_18_hat_front,
    R.drawable.hair_19_hat_front, R.drawable.hair_20_hat_front, R.drawable.hair_21_hat_front,
    R.drawable.hair_22_hat_front, R.drawable.hair_23_hat_front, R.drawable.hair_24_hat_front,
    R.drawable.hair_25_hat_front, R.drawable.hair_26_hat_front, R.drawable.hair_27_hat_front,
    R.drawable.hair_28_hat_front, R.drawable.hair_29_hat_front, R.drawable.hair_30_hat_front,
    R.drawable.hair_31_hat_front, R.drawable.hair_32_hat_front, R.drawable.hair_33_hat_front
)

// Only some hairstyles have a sprite behind the body.
private val HAIR_BACK = mapOf(
    2 to R.drawable.hair_2_back, 3 to R.drawable.hair_3_back, 4 to R.drawable.hair_4_back,
    6 to R.drawable.hair_6_back, 8 to R.drawable.hair_8_back, 9 to R.drawable.hair_9_back,
    10 to R.drawable.hair_10_back, 12 to R.drawable.hair_12_back, 14 to R.drawable.hair_14_back,
    16 to R.drawable.hair_16_back, 20 to R.drawable.hair_20_back, 21 to R.drawable.hair_21_back,
    23 to R.drawable.hair_23_back, 24 to R.drawable.hair_24_back, 26 to R.drawable.hair_26_back,
    27 to R.drawable.hair_27_back, 28 to R.drawable.hair_28_back, 31 to R.drawable.hair_31_back,
    32 to R.drawable.hair_32_back, 33 to R.drawable.hair_33_back
)
private val HAIR_BACK_TRIMMED = mapOf(
    2 to R.drawable.hair_2_hat_back, 3 to R.drawable.hair_3_hat_back, 8 to R.drawable.hair_8_hat_back,
    9 to R.drawable.hair_9_hat_back, 10 to R.drawable.hair_10_hat_back,
    12 to R.drawable.hair_12_hat_back, 14 to R.drawable.hair_14_hat_back,
    16 to R.drawable.hair_16_hat_back, 20 to R.drawable.hair_20_hat_back,
    21 to R.drawable.hair_21_hat_back, 23 to R.drawable.hair_23_hat_back,
    24 to R.drawable.hair_24_hat_back, 26 to R.drawable.hair_26_hat_back,
    27 to R.drawable.hair_27_hat_back, 28 to R.drawable.hair_28_hat_back,
    31 to R.drawable.hair_31_hat_back, 32 to R.drawable.hair_32_hat_back,
    33 to R.drawable.hair_33_hat_back
)

private fun hairEffectOf(equipment: Map<ItemSlot, CatalogItem>): HairEffect =
    equipment.values.maxOfOrNull { it.hairEffect } ?: HairEffect.NONE

/** Drawable for one layer, or null when the layer is empty for this config and equipment. */
@DrawableRes
private fun layerRes(
    layer: AvatarLayer,
    config: AvatarConfig,
    equipment: Map<ItemSlot, CatalogItem>
): Int? {
    val male = config.gender == Gender.MALE
    val hair = hairEffectOf(equipment)
    val hairIndex = config.hairStyle.coerceIn(1, AvatarConfig.HAIR_STYLES)
    return when (layer) {
        AvatarLayer.CLOAK_BACK -> equipment[ItemSlot.CLOAK]?.backSpriteRes

        AvatarLayer.HAIR_BACK -> when (hair) {
            HairEffect.HIDDEN -> null
            // Hairstyles without a trimmed back sprite keep the normal one.
            HairEffect.TRIMMED -> HAIR_BACK_TRIMMED[hairIndex] ?: HAIR_BACK[hairIndex]
            HairEffect.NONE -> HAIR_BACK[hairIndex]
        }

        AvatarLayer.BODY -> {
            val tone = config.skinTone.coerceIn(1, AvatarConfig.SKIN_TONES) - 1
            if (male) MALE_BODIES[tone] else FEMALE_BODIES[tone]
        }

        AvatarLayer.EYES -> {
            val style = config.eyeStyle.coerceIn(1, AvatarConfig.EYE_STYLES) - 1
            if (male) MALE_EYES[style] else FEMALE_EYES[style]
        }

        // An equipped outfit replaces the basic one (falls back if it has no sprite for this gender).
        AvatarLayer.OUTFIT -> equipment[ItemSlot.OUTFIT]?.spriteFor(config.gender)
            ?: if (male) R.drawable.outfit_male_basic else R.drawable.outfit_female_basic

        AvatarLayer.HAIR_FRONT -> when (hair) {
            HairEffect.HIDDEN -> null
            HairEffect.TRIMMED -> HAIR_FRONT_TRIMMED[hairIndex - 1]
            HairEffect.NONE -> HAIR_FRONT[hairIndex - 1]
        }

        AvatarLayer.HAT -> equipment[ItemSlot.HAT]?.spriteRes
        AvatarLayer.GLASSES -> equipment[ItemSlot.GLASSES]?.spriteRes
        AvatarLayer.CLOAK -> equipment[ItemSlot.CLOAK]?.spriteRes
    }
}

/**
 * Pixel-art avatar: stacked 32x32 sprites drawn with nearest-neighbor scaling.
 * Breathing moves head and torso by 1 sprite pixel; the legs stay fixed.
 * [headCrop] shows only head and shoulders (for small previews of hair, eyes and hats).
 */
@Composable
fun AvatarView(
    config: AvatarConfig,
    modifier: Modifier = Modifier,
    animated: Boolean = true,
    equipment: Map<ItemSlot, CatalogItem> = emptyMap(),
    headCrop: Boolean = false
) {
    // Layers that are empty for this config (e.g. no hair back, empty slots) are skipped.
    val bitmaps: Map<AvatarLayer, ImageBitmap> = AvatarLayer.entries.mapNotNull { layer ->
        layerRes(layer, config, equipment)?.let { layer to ImageBitmap.imageResource(it) }
    }.toMap()

    // Read inside the draw block, so breathing redraws without recomposing.
    val breathing: State<Int>? = if (animated && !headCrop) rememberBreathingBob() else null

    val hairTint = ColorFilter.tint(Color(config.hairColor), BlendMode.Modulate)

    Canvas(modifier = modifier.aspectRatio(1f)) {
        val bobPixels = breathing?.value ?: 0
        val visible = if (headCrop) CROP_SIZE else SPRITE_SIZE
        val scale = (size.minDimension / visible).toInt().coerceAtLeast(1)
        val drawn = scale * visible
        val origin = IntOffset(
            x = ((size.width - drawn) / 2).toInt(),
            y = ((size.height - drawn) / 2).toInt()
        )
        AvatarLayer.entries.forEach { layer ->
            val bitmap = bitmaps[layer] ?: return@forEach
            val isHair = layer == AvatarLayer.HAIR_BACK || layer == AvatarLayer.HAIR_FRONT
            val tint = if (isHair) hairTint else null
            if (headCrop) drawCropped(bitmap, origin, scale, tint)
            else drawSprite(bitmap, origin, scale, bobPixels, tint)
        }
    }
}

/** 0 or 1: how many sprite pixels the upper body is lowered. Snaps to whole pixels so art never blurs. */
@Composable
private fun rememberBreathingBob(): State<Int> {
    val transition = rememberInfiniteTransition(label = "breathing")
    val phase = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Reverse),
        label = "breathPhase"
    )
    return remember { derivedStateOf { if (phase.value < 0.5f) 0 else 1 } }
}

/**
 * Draws one 32x32 layer. While inhaling ([bob] = 1) the head and torso (rows above [LEGS_ROW])
 * drop 1 sprite pixel and the legs stay put, so it reads as breathing instead of floating.
 * Source row [LEGS_ROW] - 1 is skipped in that frame so the shifted part never spills over the legs.
 */
private fun DrawScope.drawSprite(
    bitmap: ImageBitmap,
    origin: IntOffset,
    scale: Int,
    bob: Int,
    colorFilter: ColorFilter?
) {
    fun drawRows(srcTop: Int, srcRows: Int, dstTopRow: Int) {
        drawImage(
            image = bitmap,
            srcOffset = IntOffset(0, srcTop),
            srcSize = IntSize(SPRITE_SIZE, srcRows),
            dstOffset = IntOffset(origin.x, origin.y + dstTopRow * scale),
            dstSize = IntSize(SPRITE_SIZE * scale, srcRows * scale),
            colorFilter = colorFilter,
            filterQuality = FilterQuality.None
        )
    }

    if (bob == 0) {
        drawRows(srcTop = 0, srcRows = SPRITE_SIZE, dstTopRow = 0)
    } else {
        drawRows(srcTop = 0, srcRows = LEGS_ROW - 1, dstTopRow = 1)
        drawRows(srcTop = LEGS_ROW, srcRows = SPRITE_SIZE - LEGS_ROW, dstTopRow = LEGS_ROW)
    }
}

/** Draws only the head-and-shoulders region of a layer, enlarged. */
private fun DrawScope.drawCropped(
    bitmap: ImageBitmap,
    origin: IntOffset,
    scale: Int,
    colorFilter: ColorFilter?
) {
    drawImage(
        image = bitmap,
        srcOffset = IntOffset(CROP_X, CROP_Y),
        srcSize = IntSize(CROP_SIZE, CROP_SIZE),
        dstOffset = origin,
        dstSize = IntSize(CROP_SIZE * scale, CROP_SIZE * scale),
        colorFilter = colorFilter,
        filterQuality = FilterQuality.None
    )
}
