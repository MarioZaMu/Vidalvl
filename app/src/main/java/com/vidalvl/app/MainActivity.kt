package com.vidalvl.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.vidalvl.app.navigation.AppRoot
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