package com.vidalvl.app.ui.screens.profile

import android.content.pm.ApplicationInfo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vidalvl.app.domain.model.Attribute
import com.vidalvl.app.ui.components.PrimaryButton
import com.vidalvl.app.ui.components.SecondaryButton
import com.vidalvl.app.ui.screens.missions.MissionsViewModel
import com.vidalvl.app.ui.theme.ThemeMode
import com.vidalvl.app.ui.theme.label

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
        val context = LocalContext.current
        val isDebugBuild = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (isDebugBuild) DebugTools()
    }
}

/** Only shown in debug builds: lets us reach reward levels without weeks of missions. */
@Composable
private fun DebugTools(viewModel: MissionsViewModel = viewModel()) {
    var lastResult by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Debug", style = MaterialTheme.typography.titleMedium)
        Attribute.entries.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { attribute ->
                    SecondaryButton(
                        text = "+100 XP ${attribute.label()}",
                        onClick = {
                            viewModel.debugAddXp(attribute, 100) { granted ->
                                lastResult = "Ítems nuevos: $granted"
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        SecondaryButton(
            text = "Dar todos los ítems",
            onClick = { viewModel.debugGrantAllItems { granted -> lastResult = "Ítems nuevos: $granted" } },
            modifier = Modifier.fillMaxWidth()
        )
        if (lastResult.isNotEmpty()) {
            Text(lastResult, style = MaterialTheme.typography.bodyMedium)
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