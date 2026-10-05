package com.vidalvl.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) }
                    )
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