package com.vidalvl.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val PARTICLES = 10
private const val DURATION_MS = 650
private const val TRAVEL_DP = 24f

/**
 * A few square particles that fly out from the center and fade, like a pixel-art "pop".
 * Plays each time [trigger] changes to a higher value (0 means "never"); does nothing if animations are off.
 * It draws outside its parent's bounds, so place it in a Box centered over the thing that was tapped.
 */
@Composable
fun PixelBurst(trigger: Int, color: Color, modifier: Modifier = Modifier) {
    val enabled = animationsEnabled()
    val progress = remember { Animatable(1f) }

    LaunchedEffect(trigger) {
        if (enabled && trigger > 0) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(DURATION_MS, easing = LinearEasing))
        }
    }

    val p = progress.value
    if (p >= 1f) return

    Canvas(modifier = modifier.requiredSize(64.dp)) {
        val particle = 4.dp.toPx()
        val grid = 2.dp.toPx()
        // Fast at first, then slowing down.
        val eased = 1f - (1f - p) * (1f - p)
        val reach = TRAVEL_DP.dp.toPx() * eased
        for (i in 0 until PARTICLES) {
            // Slightly uneven angles and speeds so it reads as a burst, not a ring.
            val angle = (2.0 * PI * i / PARTICLES) + (i % 3) * 0.12
            val speed = 0.7f + 0.3f * ((i * 7) % 5) / 4f
            val x = snap(center.x + cos(angle).toFloat() * reach * speed - particle / 2f, grid)
            val y = snap(center.y + sin(angle).toFloat() * reach * speed - particle / 2f, grid)
            drawRect(color = color.copy(alpha = 1f - p), topLeft = Offset(x, y), size = Size(particle, particle))
        }
    }
}
