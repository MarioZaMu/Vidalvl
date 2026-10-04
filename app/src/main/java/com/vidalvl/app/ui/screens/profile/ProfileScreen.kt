package com.vidalvl.app.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vidalvl.app.ui.components.PrimaryButton
import com.vidalvl.app.ui.components.SecondaryButton
import com.vidalvl.app.ui.theme.ThemeMode

// Temporary profile screen: only the appearance selector for now.
@Composable
fun ProfileScreen(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Perfil", style = MaterialTheme.typography.headlineLarge)
        Text("Apariencia", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeOption("Sistema", ThemeMode.SYSTEM, themeMode, onThemeModeChange)
            ThemeOption("Claro", ThemeMode.LIGHT, themeMode, onThemeModeChange)
            ThemeOption("Oscuro", ThemeMode.DARK, themeMode, onThemeModeChange)
        }
    }
}

@Composable
private fun RowScope.ThemeOption(
    label: String,
    mode: ThemeMode,
    selectedMode: ThemeMode,
    onSelect: (ThemeMode) -> Unit
) {
    val modifier = Modifier.weight(1f)
    if (mode == selectedMode) {
        PrimaryButton(label, onClick = { onSelect(mode) }, modifier = modifier)
    } else {
        SecondaryButton(label, onClick = { onSelect(mode) }, modifier = modifier)
    }
}