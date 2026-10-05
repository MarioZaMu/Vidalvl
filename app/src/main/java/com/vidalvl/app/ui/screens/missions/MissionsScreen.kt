package com.vidalvl.app.ui.screens.missions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vidalvl.app.R
import com.vidalvl.app.data.local.MissionEntity
import com.vidalvl.app.domain.model.AchievementUnlock
import com.vidalvl.app.domain.model.CatalogItem
import com.vidalvl.app.domain.model.ItemCatalog
import com.vidalvl.app.domain.model.MissionType
import com.vidalvl.app.domain.model.MotivationInput
import com.vidalvl.app.domain.model.MotivationRules
import com.vidalvl.app.ui.avatar.AvatarView
import com.vidalvl.app.ui.avatar.ItemSprite
import com.vidalvl.app.ui.components.MissionCard
import com.vidalvl.app.ui.components.MotivationBanner
import com.vidalvl.app.ui.components.PlayerHeader
import com.vidalvl.app.ui.theme.VidalvlTheme
import com.vidalvl.app.ui.theme.forAttribute
import com.vidalvl.app.ui.theme.descriptionRes
import com.vidalvl.app.ui.theme.iconRes
import com.vidalvl.app.ui.theme.label
import com.vidalvl.app.ui.theme.labelRes
import com.vidalvl.app.ui.theme.titleRes
import kotlinx.coroutines.delay
import java.time.LocalDate

/** The "+8 XP · Cuerpo" chip that floats up after completing a mission. */
private data class XpPop(val token: Int, val xp: Int, val label: String)

@Composable
fun MissionsScreen(viewModel: MissionsViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val player by viewModel.player.collectAsState()
    val consentGiven by viewModel.consentGiven.collectAsState()
    val colors = VidalvlTheme.attributeColors
    val context = LocalContext.current
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val missions = if (selectedTab == 0) state.daily else state.weekly
    val (activeAll, completed) = missions.partition { !it.completed }
    // Suggested missions get their own section; default and user missions stay under "Active".
    val (suggested, active) = activeAll.partition { it.mission.generatedFor != null }

    // The day's ring, the motivation line and the "perfect day" achievement count the default and the user's
    // own daily missions. Suggested ones are a bonus: they never make a day look unfinished.
    val regularDaily = state.daily.filter { it.mission.generatedFor == null }

    // The line under the header follows what is happening today, and changes as missions get done.
    val motivation = MotivationRules.pick(
        MotivationInput(
            dailyDone = regularDaily.count { it.completed },
            dailyTotal = regularDaily.size,
            streak = player.streak,
            daysSinceLast = player.daysSinceLast,
            goals = player.goals,
            neglected = MotivationRules.neglected(
                glows = state.attributes.associate { it.attribute to it.glow },
                goals = player.goals
            ),
            seed = LocalDate.now().toEpochDay()
        )
    )

    val generating by viewModel.generating.collectAsState()
    val needsSuggestionChoice by viewModel.needsSuggestionChoice.collectAsState()
    // Coming back to the app (or a new day starting while it was closed) makes today's suggestions if missing.
    LifecycleResumeEffect(Unit) {
        viewModel.refreshSuggestions()
        viewModel.refreshAchievements()
        onPauseOrDispose { }
    }

    var editor by remember { mutableStateOf<EditorState?>(null) }
    var actionsFor by remember { mutableStateOf<MissionItem?>(null) }
    var deleteFor by remember { mutableStateOf<MissionItem?>(null) }

    var xpPop by remember { mutableStateOf<XpPop?>(null) }
    var popVisible by remember { mutableStateOf(false) }
    LaunchedEffect(xpPop?.token) {
        if (xpPop != null) {
            popVisible = true
            delay(1400)
            popVisible = false
        }
    }

    var photoFor by remember { mutableStateOf<MissionItem?>(null) }

    fun complete(entry: MissionItem) {
        viewModel.toggle(entry)
        if (!entry.completed) {
            xpPop = XpPop(
                token = (xpPop?.token ?: 0) + 1,
                xp = entry.mission.xpReward,
                label = context.getString(entry.mission.attribute.labelRes())
            )
        }
    }

    fun onToggle(entry: MissionItem) {
        // Completing a photo mission goes through the camera first; undoing never needs it.
        if (!entry.completed && entry.mission.requiresPhoto) {
            photoFor = entry
            return
        }
        complete(entry)
    }

    @Composable
    fun MissionRow(entry: MissionItem, modifier: Modifier = Modifier) {
        MissionCard(
            title = entry.mission.title,
            subtitle = if (entry.mission.requiresPhoto) stringResource(R.string.mission_photo_hint)
            else entry.mission.description,
            attributeLabel = entry.mission.attribute.label(),
            attributeColor = colors.forAttribute(entry.mission.attribute),
            iconRes = entry.mission.attribute.iconRes(),
            xp = entry.mission.xpReward,
            completed = entry.completed,
            onToggle = { onToggle(entry) },
            onLongClick = { actionsFor = entry },
            onEdit = { editor = EditorState.Edit(entry.mission) },
            onDelete = { deleteFor = entry },
            modifier = modifier
        )
    }

    Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            // Extra bottom space so the add button never hides the last mission.
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "title") {
                Text(
                    stringResource(R.string.missions_title),
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.semantics { heading() }
                )
            }
            item(key = "player") {
                PlayerHeader(
                    name = player.name,
                    level = state.overallLevel.level,
                    xpIntoLevel = state.overallLevel.xpIntoLevel,
                    xpForNextLevel = state.overallLevel.xpForNextLevel,
                    levelProgress = state.overallLevel.progress,
                    dailyDone = regularDaily.count { it.completed },
                    dailyTotal = regularDaily.size,
                    streak = player.streak,
                    totalXp = state.totalXp,
                    itemCount = player.itemCount,
                    itemTotal = ItemCatalog.items.size
                ) {
                    AvatarView(
                        config = player.config,
                        equipment = player.equipped,
                        animated = false,
                        headCrop = true,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            item(key = "motivation") { MotivationBanner(motivation) }
            item(key = "tabs") { MissionTabs(selectedTab) { selectedTab = it } }

            if (generating) {
                item(key = "generating") { GeneratingNotice() }
            }

            if (active.isNotEmpty()) {
                item(key = "header_active") {
                    SectionHeader(stringResource(R.string.missions_active, active.size))
                }
                items(active, key = { it.mission.id }) { entry ->
                    MissionRow(entry, Modifier.animateItem())
                }
            }
            if (suggested.isNotEmpty()) {
                item(key = "header_suggested") {
                    SectionHeader(
                        stringResource(
                            if (selectedTab == 0) R.string.suggestions_today else R.string.suggestions_week,
                            suggested.size
                        )
                    )
                }
                items(suggested, key = { it.mission.id }) { entry ->
                    MissionRow(entry, Modifier.animateItem())
                }
            }
            if (completed.isNotEmpty()) {
                item(key = "header_completed") {
                    SectionHeader(stringResource(R.string.missions_completed, completed.size))
                }
                items(completed, key = { it.mission.id }) { entry ->
                    MissionRow(entry, Modifier.animateItem())
                }
            }
        }

        FloatingActionButton(
            onClick = { editor = EditorState.Create },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.mission_add))
        }

        AnimatedVisibility(
            visible = popVisible,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { -it / 2 }
        ) {
            xpPop?.let { pop ->
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primary,
                    shadowElevation = 6.dp
                ) {
                    Text(
                        text = stringResource(R.string.mission_xp_pop, pop.xp, pop.label),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }

    val newItems by viewModel.newItems.collectAsState()
    val newAchievements by viewModel.newAchievements.collectAsState()
    // One dialog at a time: items first, then the achievements.
    if (newItems.isNotEmpty()) {
        NewItemsDialog(newItems, onDismiss = viewModel::dismissNewItems)
    } else if (newAchievements.isNotEmpty()) {
        AchievementsDialog(newAchievements, onDismiss = viewModel::dismissNewAchievements)
    }

    if (needsSuggestionChoice) {
        // No way to dismiss without choosing: the answer decides whether anything is sent to Google.
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.suggestions_consent_title)) },
            text = { Text(stringResource(R.string.suggestions_consent_text)) },
            confirmButton = {
                TextButton(onClick = { viewModel.chooseSuggestions(useAi = true) }) {
                    Text(stringResource(R.string.suggestions_consent_ai))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.chooseSuggestions(useAi = false) }) {
                    Text(stringResource(R.string.suggestions_consent_local))
                }
            }
        )
    }

    when (val current = editor) {
        null -> Unit
        EditorState.Create -> MissionEditorSheet(
            editing = null,
            defaultType = if (selectedTab == 0) MissionType.DAILY else MissionType.WEEKLY,
            onDismiss = { editor = null },
            onCreate = { title, description, attribute, type, requiresPhoto ->
                viewModel.createMission(title, description, attribute, type, requiresPhoto)
                // Show the new mission: jump to the tab it belongs to.
                selectedTab = if (type == MissionType.DAILY) 0 else 1
                editor = null
            },
            onUpdate = { _, _, _ -> }
        )

        is EditorState.Edit -> MissionEditorSheet(
            editing = current.mission,
            defaultType = current.mission.type,
            onDismiss = { editor = null },
            onCreate = { _, _, _, _, _ -> },
            onUpdate = { title, description, requiresPhoto ->
                viewModel.updateMission(current.mission, title, description, requiresPhoto)
                editor = null
            }
        )
    }

    photoFor?.let { entry ->
        PhotoVerificationDialog(
            missionTitle = entry.mission.title,
            needsConsent = !consentGiven,
            onConsent = viewModel::giveConsent,
            verify = { jpeg -> viewModel.verifyPhoto(jpeg, entry.mission) },
            onVerified = {
                photoFor = null
                complete(entry)
            },
            onDismiss = { photoFor = null }
        )
    }

    actionsFor?.let { entry ->
        MissionActionsSheet(
            title = entry.mission.title,
            onDismiss = { actionsFor = null },
            onEdit = {
                actionsFor = null
                editor = EditorState.Edit(entry.mission)
            },
            onDelete = {
                actionsFor = null
                deleteFor = entry
            }
        )
    }

    deleteFor?.let { entry ->
        AlertDialog(
            onDismissRequest = { deleteFor = null },
            title = { Text(stringResource(R.string.mission_delete_title)) },
            text = { Text(stringResource(R.string.mission_delete_text)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteMission(entry.mission)
                    deleteFor = null
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deleteFor = null }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }
}

private sealed interface EditorState {
    data object Create : EditorState
    data class Edit(val mission: MissionEntity) : EditorState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MissionActionsSheet(
    title: String,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.navigationBarsPadding().padding(bottom = 8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.action_edit)) },
                modifier = Modifier.clickable(role = Role.Button, onClick = onEdit)
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.action_delete)) },
                modifier = Modifier.clickable(role = Role.Button, onClick = onDelete)
            )
        }
    }
}

@Composable
private fun AchievementsDialog(unlocks: List<AchievementUnlock>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.reward_ok)) }
        },
        title = {
            Text(
                stringResource(
                    if (unlocks.size == 1) R.string.achievement_unlocked_title else R.string.achievement_unlocked_title_many
                )
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                unlocks.forEach { unlock ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_trophy),
                                contentDescription = null,
                                tint = VidalvlTheme.attributeColors.accentText,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(unlock.achievement.titleRes()), style = MaterialTheme.typography.titleMedium)
                            Text(
                                stringResource(unlock.achievement.descriptionRes()),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    unlock.item?.let { prize ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ItemSprite(
                                item = prize,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            )
                            Text(
                                stringResource(R.string.achievement_prize, stringResource(prize.nameRes)),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun GeneratingNotice() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        Text(
            text = stringResource(R.string.suggestions_loading),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp).semantics { heading() }
    )
}

@Composable
private fun NewItemsDialog(items: List<CatalogItem>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.reward_ok)) }
        },
        title = {
            Text(stringResource(if (items.size == 1) R.string.reward_title else R.string.reward_title_many))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items.forEach { item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ItemSprite(
                            item = item,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                        Text(stringResource(item.nameRes), style = MaterialTheme.typography.titleMedium)
                    }
                }
                Text(
                    stringResource(R.string.reward_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

@Composable
private fun MissionTabs(selectedTab: Int, onSelect: (Int) -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(R.string.missions_tab_daily, R.string.missions_tab_weekly).forEachIndexed { index, titleRes ->
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
                    .clickable(role = Role.Tab) { onSelect(index) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(titleRes),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
