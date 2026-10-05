package com.vidalvl.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Resolves whether the app should be drawn dark for the selected mode. */
@Composable
fun ThemeMode.resolveDark(): Boolean = when (this) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

/**
 * One color per attribute, with a tone adjusted for each mode so the text stays readable on cards.
 * [accentText] is for emphasized text and small icons: the primary color is too dim for that on dark backgrounds.
 */
@Immutable
data class AttributeColors(
    val body: Color,
    val mind: Color,
    val discipline: Color,
    val social: Color,
    val xp: Color,
    val accentText: Color
)

internal val DarkAttributeColors = AttributeColors(
    body = Color(0xFFFF8A4C),
    mind = Color(0xFF5DADFF),
    discipline = Color(0xFFF5B83D),
    social = Color(0xFF3DDC9E),
    xp = DarkAccent,
    accentText = DarkAccent
)

internal val LightAttributeColors = AttributeColors(
    body = Color(0xFF9C3005),
    mind = Color(0xFF3B33B0),
    discipline = Color(0xFF7A4A00),
    social = Color(0xFF0A5C3E),
    xp = LightPrimary,
    accentText = LightPrimary
)

private val LocalAttributeColors = staticCompositionLocalOf { DarkAttributeColors }

// Material 3 picks the dialog, sheet and bar colors from the surfaceContainer family, so all of it is set
// explicitly: otherwise they would fall back to the default purple-gray tones.
internal val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = Color.White,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkTextPrimary,
    secondaryContainer = DarkPrimaryContainer,
    onSecondaryContainer = DarkAccent,
    tertiary = DarkAccent,
    onTertiary = DarkBackground,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    surfaceContainerLowest = DarkBackground,
    surfaceContainerLow = Color(0xFF16141C),
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = Color(0xFF211E2B),
    surfaceContainerHighest = DarkSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutline,
    error = DarkError
)

internal val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = Color.White,
    primaryContainer = LightSurfaceVariant,
    onPrimaryContainer = LightTextPrimary,
    secondaryContainer = LightPrimary,
    onSecondaryContainer = Color.White,
    tertiary = LightAccent,
    onTertiary = LightTextPrimary,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    surfaceContainerLowest = LightBackground,
    surfaceContainerLow = Color(0xFFF0E7D3),
    surfaceContainer = LightSurface,
    surfaceContainerHigh = Color(0xFFE5DABE),
    surfaceContainerHighest = LightSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutline,
    error = LightError
)

object VidalvlTheme {
    val attributeColors: AttributeColors
        @Composable @ReadOnlyComposable
        get() = LocalAttributeColors.current
}

@Composable
fun VidalvlTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = themeMode.resolveDark()
    CompositionLocalProvider(
        LocalAttributeColors provides if (darkTheme) DarkAttributeColors else LightAttributeColors
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = Typography,
            content = content
        )
    }
}
