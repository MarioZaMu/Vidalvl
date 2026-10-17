package com.vidalvl.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

/**
 * Limits how much the system font size grows inside [content]. Meant for text that sits in a fixed-size graphic
 * (a ring, a chart label) where it would spill out; the same information is always described for accessibility.
 */
@Composable
fun CappedFontScale(max: Float, content: @Composable () -> Unit) {
    val density = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(density.density, minOf(density.fontScale, max))) {
        content()
    }
}
