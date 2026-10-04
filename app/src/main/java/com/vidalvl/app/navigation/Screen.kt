package com.vidalvl.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

/** App destinations shown in the bottom bar. */
sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object Missions : Screen("missions", "Misiones", Icons.Filled.Home)
    data object Creature : Screen("creature", "Criatura", Icons.Filled.Face)
    data object Reflection : Screen("reflection", "Reflejo", Icons.Filled.DateRange)
    data object Profile : Screen("profile", "Perfil", Icons.Filled.Person)

    companion object {
        val bottomBarItems = listOf(Missions, Creature, Reflection, Profile)
    }
}