package com.vidalvl.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import java.util.Random
import kotlin.math.PI
import kotlin.math.sin

private const val STAR_COUNT = 70
private const val MOON_GRID = 12

private data class Star(
    val x: Float,
    val y: Float,
    /** Size in pixels (1 or 2). */
    val size: Int,
    val phase: Float,
    val brightness: Float,
    val warm: Boolean
)

private val StarWhite = Color(0xFFF2ECE4)
private val StarEmber = Color(0xFFF0A04B)
private val MoonColor = Color(0xFFE8DFC9)

/**
 * A pixel-art night sky for the dark theme: stars that twinkle slowly and a crescent moon, all drawn in code.
 * It stops twinkling when the system animations are turned off.
 */
@Composable
fun NightSky(modifier: Modifier = Modifier) {
    val animated = animationsEnabled()
    val stars = remember { buildStars() }
    val twinkle: State<Float>? = if (animated) {
        rememberInfiniteTransition(label = "stars").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Restart),
            label = "twinkle"
        )
    } else null

    // The twinkle value is read inside the draw block, so the sky redraws without recomposing anything.
    Canvas(modifier = modifier) {
        val pixel = 2.dp.toPx()
        val t = twinkle?.value ?: 0f
        stars.forEach { star ->
            val wave = if (animated) 0.5f + 0.5f * sin((t + star.phase) * 2.0 * PI).toFloat() else 1f
            val alpha = star.brightness * (0.3f + 0.7f * wave)
            drawRect(
                color = (if (star.warm) StarEmber else StarWhite).copy(alpha = alpha),
                topLeft = Offset(snap(star.x * size.width, pixel), snap(star.y * size.height, pixel)),
                size = Size(pixel * star.size, pixel * star.size)
            )
        }
        drawMoon(cell = 3.dp.toPx())
    }
}

/** A crescent: a disc with a second, offset disc cut out of it, on a grid so it stays pixelated. */
private fun DrawScope.drawMoon(cell: Float) {
    val left = size.width - cell * MOON_GRID - 20.dp.toPx()
    val top = 64.dp.toPx()
    for (gx in 0 until MOON_GRID) {
        for (gy in 0 until MOON_GRID) {
            val cx = gx + 0.5f
            val cy = gy + 0.5f
            val inOuter = dist(cx, cy, 6f, 6f) <= 5.5f
            val inCut = dist(cx, cy, 8.4f, 5f) <= 4.7f
            if (inOuter && !inCut) {
                drawRect(
                    color = MoonColor.copy(alpha = 0.9f),
                    topLeft = Offset(left + gx * cell, top + gy * cell),
                    size = Size(cell, cell)
                )
            }
        }
    }
}

/** Fixed positions (seeded), so the sky is the same every time. */
private fun buildStars(): List<Star> {
    val random = Random(7)
    return List(STAR_COUNT) {
        Star(
            x = random.nextFloat(),
            y = random.nextFloat(),
            size = if (random.nextInt(5) == 0) 2 else 1,
            phase = random.nextFloat(),
            brightness = 0.45f + 0.55f * random.nextFloat(),
            warm = random.nextInt(6) == 0
        )
    }
}
