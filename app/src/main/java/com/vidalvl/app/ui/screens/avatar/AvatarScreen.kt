package com.vidalvl.app.ui.screens.avatar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vidalvl.app.R
import com.vidalvl.app.domain.model.AchievementRules
import com.vidalvl.app.domain.model.AvatarConfig
import com.vidalvl.app.domain.model.CatalogItem
import com.vidalvl.app.domain.model.ItemCatalog
import com.vidalvl.app.domain.model.ItemSlot
import com.vidalvl.app.domain.model.ItemUnlock
import com.vidalvl.app.ui.avatar.AvatarView
import com.vidalvl.app.ui.components.LevelPill
import com.vidalvl.app.ui.components.PrimaryButton
import com.vidalvl.app.ui.components.RadarChart
import com.vidalvl.app.ui.components.RadarEntry
import com.vidalvl.app.ui.components.SecondaryButton
import com.vidalvl.app.ui.components.StatBar
import com.vidalvl.app.ui.screens.missions.AttributeUi
import com.vidalvl.app.ui.theme.VidalvlTheme
import com.vidalvl.app.ui.theme.forAttribute
import com.vidalvl.app.ui.theme.iconRes
import com.vidalvl.app.ui.theme.label
import com.vidalvl.app.ui.theme.titleRes

// Slots shown as inventory tabs.
private val InventorySlots = listOf(ItemSlot.HAT, ItemSlot.GLASSES, ItemSlot.CLOAK, ItemSlot.OUTFIT)

/** Avatar tab: creator the first time, then the avatar with its inventory. */
@Composable
fun AvatarScreen(viewModel: AvatarViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    var editing by rememberSaveable { mutableStateOf(false) }

    when (val current = state) {
        AvatarUiState.Loading -> Unit
        AvatarUiState.NotCreated -> AvatarCreatorScreen(
            initial = AvatarConfig(),
            isEditing = false,
            onSave = viewModel::save,
            onCancel = {}
        )

        is AvatarUiState.Created -> if (editing) {
            AvatarCreatorScreen(
                initial = current.config,
                isEditing = true,
                equipment = current.equipped,
                onSave = {
                    viewModel.save(it)
                    editing = false
                },
                onCancel = { editing = false }
            )
        } else {
            AvatarHome(current, onEdit = { editing = true }, onToggleEquip = viewModel::toggleEquip)
        }
    }
}

@Composable
private fun AvatarHome(
    state: AvatarUiState.Created,
    onEdit: () -> Unit,
    onToggleEquip: (CatalogItem) -> Unit
) {
    val description = stringResource(R.string.avatar_content_description)
    val attributeColors = VidalvlTheme.attributeColors
    var slotIndex by rememberSaveable { mutableStateOf(0) }
    val slot = InventorySlots[slotIndex]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(stringResource(R.string.avatar_title), style = MaterialTheme.typography.headlineLarge)

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AvatarView(
                            config = state.config,
                            equipment = state.equipped,
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { contentDescription = description }
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LevelPill(state.overallLevel)
                            Text(
                                text = state.config.name.ifBlank { stringResource(R.string.player_default_name) },
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // The chart shows each attribute's level against the best one (at least level 5).
                    val scale = maxOf(5, state.attributes.maxOf { it.level.level })
                    val radarDescription = state.attributes
                        .map { stringResource(R.string.item_req_attribute, it.attribute.label(), it.level.level) }
                        .joinToString(", ")
                    RadarChart(
                        entries = state.attributes.map {
                            RadarEntry(
                                levelText = it.level.level.toString(),
                                iconRes = it.attribute.iconRes(),
                                color = attributeColors.forAttribute(it.attribute),
                                value = it.level.level.toFloat() / scale
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .semantics { contentDescription = radarDescription }
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    state.attributes.forEach { entry ->
                        AttributeRow(entry, attributeColors.forAttribute(entry.attribute))
                    }
                }
            }
        }

        SecondaryButton(
            text = stringResource(R.string.avatar_edit),
            onClick = onEdit,
            modifier = Modifier.fillMaxWidth()
        )

        Text(stringResource(R.string.inventory_title), style = MaterialTheme.typography.titleLarge)

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            InventorySlots.forEachIndexed { index, itemSlot ->
                SlotTab(stringResource(slotName(itemSlot)), index == slotIndex) { slotIndex = index }
            }
        }

        val slotItems = ItemCatalog.items.filter { it.slot == slot }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            slotItems.chunked(3).forEach { rowItems ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowItems.forEach { item ->
                        ItemTile(
                            item = item,
                            config = state.config,
                            available = item.availableFor(state.config.gender),
                            owned = item.id in state.ownedIds,
                            equipped = state.equipped[item.slot]?.id == item.id,
                            onClick = { onToggleEquip(item) }
                        )
                    }
                    // Keeps tiles the same width in an incomplete last row.
                    repeat(3 - rowItems.size) { Box(modifier = Modifier.weight(1f)) }
                }
            }
        }
    }
}

/** Attribute with its icon, level and progress. Dims a little when the attribute has been neglected. */
@Composable
private fun AttributeRow(entry: AttributeUi, color: Color) {
    Row(
        modifier = Modifier.alpha(0.6f + 0.4f * entry.glow),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(entry.attribute.iconRes()),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }
        StatBar(
            label = stringResource(R.string.item_req_attribute, entry.attribute.label(), entry.level.level),
            valueText = stringResource(R.string.player_xp_value, entry.level.xpIntoLevel, entry.level.xpForNextLevel),
            progress = entry.level.progress,
            color = color,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun slotName(slot: ItemSlot): Int = when (slot) {
    ItemSlot.HAT -> R.string.slot_hat
    ItemSlot.GLASSES -> R.string.slot_glasses
    ItemSlot.CLOAK -> R.string.slot_cloak
    ItemSlot.OUTFIT -> R.string.slot_outfit
}

@Composable
private fun SlotTab(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) PrimaryButton(label, onClick = onClick)
    else SecondaryButton(label, onClick = onClick)
}

/** One inventory slot: the avatar wearing the item. Locked items are dimmed and show how to get them. */
@Composable
private fun RowScope.ItemTile(
    item: CatalogItem,
    config: AvatarConfig,
    available: Boolean,
    owned: Boolean,
    equipped: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    val name = stringResource(item.nameRes)
    val requirement = requirementText(item)
    val usable = owned && available
    val note = when {
        equipped -> stringResource(R.string.item_equipped)
        !owned -> requirement
        !available -> stringResource(R.string.item_male_only)
        else -> ""
    }
    val description = when {
        !owned -> stringResource(R.string.item_locked_description, name, requirement)
        !available -> stringResource(R.string.item_locked_description, name, note)
        else -> name
    }
    val borderColor = if (equipped) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    // Hats and glasses are small details, so their previews zoom in on the head.
    val zoomHead = item.slot == ItemSlot.HAT || item.slot == ItemSlot.GLASSES

    Column(
        modifier = Modifier
            .weight(1f)
            .clip(shape)
            .alpha(if (usable) 1f else 0.4f)
            .selectable(selected = equipped, enabled = usable, role = Role.Checkbox, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surface)
                .border(BorderStroke(if (equipped) 3.dp else 1.dp, borderColor), shape)
                .padding(4.dp)
        ) {
            AvatarView(
                config = config,
                animated = false,
                equipment = mapOf(item.slot to item),
                headCrop = zoomHead,
                modifier = Modifier.fillMaxSize()
            )
        }
        Text(
            text = name,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            maxLines = 2,
            minLines = 2
        )
        Text(
            text = note,
            style = MaterialTheme.typography.bodySmall,
            color = if (equipped) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
            minLines = 2
        )
    }
}

@Composable
private fun requirementText(item: CatalogItem): String {
    val base = when (val unlock = item.unlock) {
        is ItemUnlock.TotalLevel -> stringResource(R.string.item_req_level, unlock.level)
        is ItemUnlock.Weekly -> stringResource(R.string.item_req_weekly, item.attribute.label())
        // Special items are the prizes of achievements: say which one.
        ItemUnlock.RewardMission -> AchievementRules.givingItem(item.id)
            ?.let { stringResource(R.string.item_req_achievement, stringResource(it.titleRes())) }
            ?: stringResource(R.string.item_req_special)
    }
    return if (item.requiredAttributeLevel > 1) {
        base + " · " + stringResource(R.string.item_req_attribute, item.attribute.label(), item.requiredAttributeLevel)
    } else base
}
