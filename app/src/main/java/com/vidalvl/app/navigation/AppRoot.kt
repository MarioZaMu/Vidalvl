package com.vidalvl.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vidalvl.app.ui.screens.onboarding.GoalsState
import com.vidalvl.app.ui.screens.onboarding.GoalsViewModel
import com.vidalvl.app.ui.screens.onboarding.OnboardingScreen
import com.vidalvl.app.ui.theme.ThemeMode

/** Shows the welcome screen until the user has chosen goals, then the app. */
@Composable
fun AppRoot(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    viewModel: GoalsViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    when (state) {
        GoalsState.Loading -> Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        GoalsState.Missing -> OnboardingScreen(onFinish = viewModel::finishWelcome)
        is GoalsState.Saved -> AppNavigation(
            themeMode = themeMode,
            onThemeModeChange = onThemeModeChange,
            startOnAvatar = viewModel.startOnAvatar
        )
    }
}
