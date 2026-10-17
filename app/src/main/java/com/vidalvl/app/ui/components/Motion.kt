package com.vidalvl.app.ui.components

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlin.math.floor
import kotlin.math.sqrt

/**
 * False when the user turned system animations off (a common accessibility and battery setting).
 * Decorative motion (twinkling stars, drifting clouds, the completion burst) checks this and stays still.
 */
@Composable
fun animationsEnabled(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }
}

/** Rounds [value] down to a multiple of [step], so pixel art lands on whole cells and never blurs. */
internal fun snap(value: Float, step: Float): Float = floor(value / step) * step

internal fun dist(x1: Float, y1: Float, x2: Float, y2: Float): Float {
    val dx = x1 - x2
    val dy = y1 - y2
    return sqrt(dx * dx + dy * dy)
}
