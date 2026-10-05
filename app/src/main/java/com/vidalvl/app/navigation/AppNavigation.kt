package com.vidalvl.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vidalvl.app.ui.screens.avatar.AvatarScreen
import com.vidalvl.app.ui.screens.missions.MissionsScreen
import com.vidalvl.app.ui.screens.profile.ProfileScreen
import com.vidalvl.app.ui.screens.reflection.ReflectionScreen
import com.vidalvl.app.ui.theme.ThemeMode

private const val NAV_MAX_FONT_SCALE = 1.3f

@Composable
fun AppNavigation(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    startOnAvatar: Boolean = false
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        // Transparent so the night sky (dark theme) shows through behind the screens.
        containerColor = Color.Transparent,
        bottomBar = {
            // Four fixed-width tabs cannot hold labels at 200% font: they would break mid-word ("Misione/s").
            // Their text stops growing at 1.3x; everything else on screen keeps following the system setting.
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale = minOf(density.fontScale, NAV_MAX_FONT_SCALE))
            ) {
                NavigationBar {
                    Screen.bottomBarItems.forEach { screen ->
                        NavigationBarItem(
                            selected = currentRoute == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(screen.icon, contentDescription = null) },
                            label = { Text(stringResource(screen.labelRes), maxLines = 1) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            // Right after the welcome screen the next step is creating the avatar.
            startDestination = if (startOnAvatar) Screen.Avatar.route else Screen.Missions.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Missions.route) { MissionsScreen() }
            composable(Screen.Avatar.route) { AvatarScreen() }
            composable(Screen.Reflection.route) { ReflectionScreen() }
            composable(Screen.Profile.route) { ProfileScreen(themeMode, onThemeModeChange) }
        }
    }
}