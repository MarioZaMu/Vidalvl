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

/** One color per attribute, with a tone adjusted for each mode. */
@Immutable
data class AttributeColors(
    val body: Color,
    val mind: Color,
    val discipline: Color,
    val social: Color,
    val xp: Color
)

private val DarkAttributeColors = AttributeColors(
    body = Color(0xFFFF8A4C),
    mind = Color(0xFF4DA3FF),
    discipline = Color(0xFFF5B83D),
    social = Color(0xFF34D399),
    xp = DarkAccent
)

private val LightAttributeColors = AttributeColors(
    body = Color(0xFFD9480F),
    mind = Color(0xFF4F46C8),
    discipline = Color(0xFF9A5B00),
    social = Color(0xFF0B7A55),
    xp = LightPrimary
)

private val LocalAttributeColors = staticCompositionLocalOf { DarkAttributeColors }

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = Color.White,
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
    surfaceContainer = DarkSurface,
    outline = DarkOutline,
    error = DarkError
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = Color.White,
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
    surfaceContainer = LightSurface,
    outline = LightOutline,
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