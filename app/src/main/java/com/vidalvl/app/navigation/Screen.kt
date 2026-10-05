package com.vidalvl.app.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector
import com.vidalvl.app.R

/** App destinations shown in the bottom bar. The label is a string resource, so it follows the app language. */
sealed class Screen(val route: String, @StringRes val labelRes: Int, val icon: ImageVector) {
    data object Missions : Screen("missions", R.string.nav_missions, Icons.Filled.Home)
    data object Avatar : Screen("avatar", R.string.nav_avatar, Icons.Filled.Face)
    data object Reflection : Screen("reflection", R.string.nav_reflection, Icons.Filled.DateRange)
    data object Profile : Screen("profile", R.string.nav_profile, Icons.Filled.Person)

    companion object {
        val bottomBarItems = listOf(Missions, Avatar, Reflection, Profile)
    }
}
