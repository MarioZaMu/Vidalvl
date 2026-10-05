package com.vidalvl.app

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.vidalvl.app.ui.theme.AttributeColors
import com.vidalvl.app.ui.theme.DarkAttributeColors
import com.vidalvl.app.ui.theme.DarkColorScheme
import com.vidalvl.app.ui.theme.LightAttributeColors
import com.vidalvl.app.ui.theme.LightColorScheme
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/** WCAG 2.x contrast: 4.5:1 is the minimum for normal text. */
class ContrastTest {
    private fun channel(value: Float): Double {
        val v = value.toDouble()
        return if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
    }

    private fun luminance(c: Color) =
        0.2126 * channel(c.red) + 0.7152 * channel(c.green) + 0.0722 * channel(c.blue)

    private fun ratio(a: Color, b: Color): Double {
        val l1 = luminance(a)
        val l2 = luminance(b)
        return (maxOf(l1, l2) + 0.05) / (minOf(l1, l2) + 0.05)
    }

    private fun check(failures: MutableList<String>, name: String, text: Color, background: Color, min: Double = 4.5) {
        val r = ratio(text, background)
        if (r < min) failures += "%s: %.2f:1 (needs %.1f)".format(name, r, min)
    }

    private fun checkMode(mode: String, scheme: ColorScheme, attrs: AttributeColors): List<String> {
        val failures = mutableListOf<String>()
        val backgrounds = mapOf(
            "background" to scheme.background,
            "surface" to scheme.surface,
            "surfaceVariant" to scheme.surfaceVariant
        )
        backgrounds.forEach { (bgName, bg) ->
            check(failures, "$mode onSurface on $bgName", scheme.onSurface, bg)
            check(failures, "$mode secondary text on $bgName", scheme.onSurfaceVariant, bg)
            check(failures, "$mode accent text on $bgName", attrs.accentText, bg)
            check(failures, "$mode body on $bgName", attrs.body, bg)
            check(failures, "$mode mind on $bgName", attrs.mind, bg)
            check(failures, "$mode discipline on $bgName", attrs.discipline, bg)
            check(failures, "$mode social on $bgName", attrs.social, bg)
            check(failures, "$mode error on $bgName", scheme.error, bg)
        }
        check(failures, "$mode onBackground on background", scheme.onBackground, scheme.background)
        check(failures, "$mode onPrimary on primary", scheme.onPrimary, scheme.primary)
        return failures
    }

    @Test
    fun dark_palette_is_readable() {
        val failures = checkMode("dark", DarkColorScheme, DarkAttributeColors)
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun light_palette_is_readable() {
        val failures = checkMode("light", LightColorScheme, LightAttributeColors)
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }
}
