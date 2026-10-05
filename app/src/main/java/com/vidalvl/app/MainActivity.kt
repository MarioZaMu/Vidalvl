package com.vidalvl.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.vidalvl.app.navigation.AppRoot
import com.vidalvl.app.ui.components.NightSky
import com.vidalvl.app.ui.theme.ThemeMode
import com.vidalvl.app.ui.theme.ThemeStorage
import com.vidalvl.app.ui.theme.VidalvlTheme
import com.vidalvl.app.ui.theme.resolveDark

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            var themeMode by remember { mutableStateOf(ThemeStorage.load(context)) }
            val darkTheme = themeMode.resolveDark()

            // Keeps status/navigation bar icons readable in both modes.
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme }
                )
                onDispose {}
            }

            VidalvlTheme(themeMode = themeMode) {
                // The sky sits behind everything; the screens draw on top with transparent backgrounds.
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    if (darkTheme) NightSky(modifier = Modifier.fillMaxSize())
                    // With no opaque Surface around, text would default to black: set the base text color here.
                    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
                        AppRoot(
                            themeMode = themeMode,
                            onThemeModeChange = { newMode ->
                                themeMode = newMode
                                ThemeStorage.save(context, newMode)
                            }
                        )
                    }
                }
            }
        }
    }
}