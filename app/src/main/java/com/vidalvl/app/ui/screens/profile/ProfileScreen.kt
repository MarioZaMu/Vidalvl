package com.vidalvl.app.ui.screens.profile

import android.content.pm.ApplicationInfo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vidalvl.app.domain.model.Attribute
import com.vidalvl.app.ui.components.PrimaryButton
import com.vidalvl.app.ui.components.SecondaryButton
import com.vidalvl.app.R
import com.vidalvl.app.ui.screens.missions.MissionsViewModel
import com.vidalvl.app.ui.screens.onboarding.GoalsEditorSheet
import com.vidalvl.app.ui.screens.onboarding.GoalsState
import com.vidalvl.app.ui.screens.onboarding.GoalsViewModel
import com.vidalvl.app.ui.screens.onboarding.titleRes
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
        GoalsSection()
        AiSuggestionsSwitch()
        val context = LocalContext.current
        val isDebugBuild = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (isDebugBuild) DebugTools()
    }
}

/** Turns the AI suggestions on or off. Off means built-in ideas only, and nothing is sent to Google. */
@Composable
private fun AiSuggestionsSwitch(viewModel: MissionsViewModel = viewModel()) {
    val aiEnabled by viewModel.aiEnabled.collectAsState()
    val enabled = aiEnabled == true

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = enabled, role = Role.Switch, onValueChange = viewModel::setAiEnabled),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.profile_ai_title), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(R.string.profile_ai_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        // The whole row toggles, so the switch is not a second tap target.
        Switch(checked = enabled, onCheckedChange = null)
    }
}

/** The goals chosen on the welcome screen, editable at any time. */
@Composable
private fun GoalsSection(viewModel: GoalsViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    var editing by remember { mutableStateOf(false) }
    val saved = (state as? GoalsState.Saved)?.goals

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.profile_goals_title), style = MaterialTheme.typography.titleMedium)
        if (saved != null) {
            Text(
                text = saved.goals.map { stringResource(it.titleRes()) }.joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        SecondaryButton(
            text = stringResource(R.string.profile_goals_edit),
            onClick = { editing = true },
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (editing) {
        GoalsEditorSheet(
            initial = saved,
            onDismiss = { editing = false },
            onSave = {
                viewModel.save(it)
                editing = false
            }
        )
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