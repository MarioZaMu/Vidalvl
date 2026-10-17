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
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.vidalvl.app.ui.theme.CloudColor
import com.vidalvl.app.ui.theme.CloudShade
import com.vidalvl.app.ui.theme.LightBackground
import com.vidalvl.app.ui.theme.SkyTop
import com.vidalvl.app.ui.theme.SunColor
import kotlin.math.PI
import kotlin.math.sin

private const val CYCLE_MS = 120_000
private const val SUN_GRID = 15
private const val SUN_PULSES_PER_CYCLE = 20

private val CloudShapeA = listOf(
    "....####......",
    "..########.##.",
    ".############.",
    "##############"
)

private val CloudShapeB = listOf(
    "...###......",
    ".#######.##.",
    "############"
)

private class Cloud(
    val mask: List<String>,
    val cell: Float,
    val yFraction: Float,
    /** Whole trips across the screen per cycle, so the loop restarts without a jump. */
    val loops: Int,
    val phase: Float
)

private val Clouds = listOf(
    Cloud(CloudShapeA, 4f, 0.07f, 1, 0.05f),
    Cloud(CloudShapeB, 3f, 0.17f, 2, 0.55f),
    Cloud(CloudShapeA, 3f, 0.27f, 3, 0.30f),
    Cloud(CloudShapeB, 4f, 0.12f, 1, 0.70f)
)

/**
 * Left edge of a cloud for a cycle [progress] in 0..1. The cloud crosses the screen [loops] times per cycle,
 * entering fully off the left edge and leaving fully off the right one, so progress 0 and 1 give the same spot.
 */
internal fun cloudX(progress: Float, loops: Int, phase: Float, screenWidth: Float, cloudWidth: Float): Float {
    val turns = progress * loops + phase
    val fraction = turns - kotlin.math.floor(turns)
    return fraction * (screenWidth + cloudWidth) - cloudWidth
}

/**
 * A pixel-art day sky for the light theme: a pale blue that melts into the parchment, a pixel sun and slow clouds.
 * Mirrors [NightSky]; everything stands still when the system animations are turned off.
 */
@Composable
fun DaySky(modifier: Modifier = Modifier) {
    val animated = animationsEnabled()
    val progress: State<Float>? = if (animated) {
        rememberInfiniteTransition(label = "day").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(CYCLE_MS, easing = LinearEasing), RepeatMode.Restart),
            label = "clouds"
        )
    } else null

    // The progress is read inside the draw block, so the sky redraws without recomposing anything.
    Canvas(modifier = modifier) {
        drawRect(
            brush = Brush.verticalGradient(
                0f to SkyTop,
                0.4f to LightBackground,
                1f to LightBackground
            )
        )
        val t = progress?.value ?: 0f
        drawSun(cell = 3.dp.toPx(), pulse = if (animated) pulseAt(t) else 1f)
        Clouds.forEach { drawCloud(it, t) }
    }
}

private fun pulseAt(t: Float): Float =
    0.65f + 0.35f * (0.5f + 0.5f * sin(t * SUN_PULSES_PER_CYCLE * 2.0 * PI).toFloat())

/** A disc plus eight short rays on a grid, centered where the moon sits at night. */
private fun DrawScope.drawSun(cell: Float, pulse: Float) {
    val centerX = size.width - 20.dp.toPx() - 18.dp.toPx()
    val centerY = 64.dp.toPx() + 18.dp.toPx()
    val left = snap(centerX - cell * SUN_GRID / 2f, cell)
    val top = snap(centerY - cell * SUN_GRID / 2f, cell)
    for (gx in 0 until SUN_GRID) {
        for (gy in 0 until SUN_GRID) {
            val inDisc = dist(gx + 0.5f, gy + 0.5f, 7.5f, 7.5f) <= 3.6f
            if (inDisc) drawCell(left, top, gx, gy, cell, SunColor)
        }
    }
    val c = SUN_GRID / 2
    val rayCells = listOf(
        c to 1, c to 2, c to 12, c to 13, 1 to c, 2 to c, 12 to c, 13 to c,
        3 to 3, 11 to 3, 3 to 11, 11 to 11
    )
    rayCells.forEach { (gx, gy) -> drawCell(left, top, gx, gy, cell, SunColor.copy(alpha = pulse)) }
}

private fun DrawScope.drawCell(left: Float, top: Float, gx: Int, gy: Int, cell: Float, color: Color) {
    drawRect(color = color, topLeft = Offset(left + gx * cell, top + gy * cell), size = Size(cell, cell))
}

private fun DrawScope.drawCloud(cloud: Cloud, progress: Float) {
    val cell = cloud.cell.dp.toPx()
    val width = cloud.mask.maxOf { it.length } * cell
    val left = snap(cloudX(progress, cloud.loops, cloud.phase, size.width, width), cell)
    val top = snap(size.height * cloud.yFraction, cell)
    cloud.mask.forEachIndexed { row, line ->
        val color = if (row == cloud.mask.lastIndex) CloudShade else CloudColor
        line.forEachIndexed { col, ch ->
            if (ch == '#') drawCell(left, top, col, row, cell, color.copy(alpha = 0.92f))
        }
    }
}
