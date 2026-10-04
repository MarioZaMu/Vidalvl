package com.vidalvl.app.ui.screens.missions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vidalvl.app.ui.components.AttributeChip
import com.vidalvl.app.ui.components.MissionCard
import com.vidalvl.app.ui.components.StatBar
import com.vidalvl.app.ui.theme.VidalvlTheme
import com.vidalvl.app.ui.theme.forAttribute
import com.vidalvl.app.ui.theme.label
import kotlinx.coroutines.launch

@Composable
fun MissionsScreen(viewModel: MissionsViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val colors = VidalvlTheme.attributeColors
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val missions = if (selectedTab == 0) state.daily else state.weekly

    fun onMissionTap(item: MissionItem) {
        viewModel.toggle(item)
        if (!item.completed) {
            scope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                val result = snackbarHostState.showSnackbar(
                    message = "+${item.mission.xpReward} XP de ${item.mission.attribute.label()}",
                    actionLabel = "Deshacer",
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.toggle(item.copy(completed = true))
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Text("Misiones", style = MaterialTheme.typography.headlineLarge) }
            item { PlayerHeader(state) }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.attributes.forEach { entry ->
                        AttributeChip(
                            label = entry.attribute.label(),
                            level = entry.level.level,
                            progress = entry.level.progress,
                            glow = entry.glow,
                            color = colors.forAttribute(entry.attribute),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            item { MissionTabs(selectedTab) { selectedTab = it } }
            items(missions, key = { it.mission.id }) { entry ->
                MissionCard(
                    title = entry.mission.title,
                    subtitle = if (entry.mission.requiresPhoto) "Se verificará con foto"
                    else entry.mission.description,
                    attributeLabel = entry.mission.attribute.label(),
                    attributeColor = colors.forAttribute(entry.mission.attribute),
                    xp = entry.mission.xpReward,
                    completed = entry.completed,
                    onToggle = { onMissionTap(entry) }
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
        )
    }
}

@Composable
private fun PlayerHeader(state: MissionsUiState) {
    val doneToday = state.daily.count { it.completed }
    val level = state.overallLevel

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Nivel ${level.level}", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Hoy: $doneToday/${state.daily.size} diarias",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StatBar(
                label = "Experiencia",
                valueText = "${level.xpIntoLevel}/${level.xpForNextLevel} XP",
                progress = level.progress,
                color = VidalvlTheme.attributeColors.xp
            )
        }
    }
}

@Composable
private fun MissionTabs(selectedTab: Int, onSelect: (Int) -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf("Diarias", "Semanales").forEachIndexed { index, title ->
            val selected = index == selectedTab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(shape)
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surface
                    )
                    .border(1.dp, MaterialTheme.colorScheme.outline, shape)
                    .clickable { onSelect(index) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}