package com.vidalvl.app.ui.screens.missions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vidalvl.app.R
import com.vidalvl.app.data.local.MissionEntity
import com.vidalvl.app.domain.model.Attribute
import com.vidalvl.app.domain.model.GameRules
import com.vidalvl.app.domain.model.MissionRules
import com.vidalvl.app.domain.model.MissionType
import com.vidalvl.app.ui.components.PrimaryButton
import com.vidalvl.app.ui.components.SecondaryButton
import com.vidalvl.app.ui.theme.VidalvlTheme
import com.vidalvl.app.ui.theme.forAttribute
import com.vidalvl.app.ui.theme.iconRes
import com.vidalvl.app.ui.theme.label

/**
 * Bottom sheet to create a mission ([editing] = null) or to edit the text of an existing one.
 * Attribute and type are chosen only when creating; XP comes from the type.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissionEditorSheet(
    editing: MissionEntity?,
    defaultType: MissionType,
    onDismiss: () -> Unit,
    onCreate: (title: String, description: String, attribute: Attribute, type: MissionType, requiresPhoto: Boolean) -> Unit,
    onUpdate: (title: String, description: String, requiresPhoto: Boolean) -> Unit
) {
    var title by rememberSaveable { mutableStateOf(editing?.title.orEmpty()) }
    var description by rememberSaveable { mutableStateOf(editing?.description.orEmpty()) }
    var attribute by rememberSaveable { mutableStateOf(editing?.attribute ?: Attribute.BODY) }
    var type by rememberSaveable { mutableStateOf(editing?.type ?: defaultType) }
    var requiresPhoto by rememberSaveable { mutableStateOf(editing?.requiresPhoto ?: false) }

    // Fully expanded from the start, so the keyboard never fights a half-open sheet.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(if (editing == null) R.string.mission_add else R.string.mission_edit_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() }
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(MissionRules.MAX_TITLE_LENGTH) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.mission_field_title)) },
                supportingText = {
                    Text(stringResource(R.string.field_counter, title.length, MissionRules.MAX_TITLE_LENGTH))
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it.take(MissionRules.MAX_DESCRIPTION_LENGTH) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.mission_field_description)) },
                supportingText = {
                    Text(
                        stringResource(R.string.field_counter, description.length, MissionRules.MAX_DESCRIPTION_LENGTH)
                    )
                },
                maxLines = 2,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
            )

            if (editing == null) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.mission_field_attribute), style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Attribute.entries.forEach { option ->
                            AttributeOption(option, selected = option == attribute) { attribute = option }
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.mission_field_type), style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TypeOption(
                            label = stringResource(R.string.mission_type_daily_xp, GameRules.missionXp(MissionType.DAILY)),
                            selected = type == MissionType.DAILY
                        ) { type = MissionType.DAILY }
                        TypeOption(
                            label = stringResource(R.string.mission_type_weekly_xp, GameRules.missionXp(MissionType.WEEKLY)),
                            selected = type == MissionType.WEEKLY
                        ) { type = MissionType.WEEKLY }
                    }
                }
            } else {
                // Shown read-only: changing them would rewrite the meaning of past completions.
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(
                            R.string.mission_fixed_info,
                            editing.attribute.label(),
                            stringResource(
                                if (editing.type == MissionType.DAILY) R.string.mission_type_daily
                                else R.string.mission_type_weekly
                            )
                        ),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(R.string.mission_fixed_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(value = requiresPhoto, role = Role.Switch, onValueChange = { requiresPhoto = it }),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.mission_requires_photo), style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = stringResource(R.string.mission_requires_photo_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // The whole row toggles, so the switch itself is not a separate tap target.
                Switch(checked = requiresPhoto, onCheckedChange = null)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryButton(
                    text = stringResource(R.string.action_cancel),
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
                PrimaryButton(
                    text = stringResource(R.string.action_save),
                    onClick = {
                        if (editing == null) onCreate(title, description, attribute, type, requiresPhoto)
                        else onUpdate(title, description, requiresPhoto)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = MissionRules.isValidTitle(title)
                )
            }
        }
    }
}

@Composable
private fun RowScope.AttributeOption(attribute: Attribute, selected: Boolean, onClick: () -> Unit) {
    val color: Color = VidalvlTheme.attributeColors.forAttribute(attribute)
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .weight(1f)
            .heightIn(min = 64.dp)
            .clip(shape)
            .background(if (selected) color.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface)
            .border(BorderStroke(if (selected) 3.dp else 1.dp, if (selected) color else MaterialTheme.colorScheme.outline), shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(attribute.iconRes()),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = attribute.label(),
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun RowScope.TypeOption(label: String, selected: Boolean, onClick: () -> Unit) {
    val modifier = Modifier.weight(1f)
    if (selected) PrimaryButton(label, onClick = onClick, modifier = modifier)
    else SecondaryButton(label, onClick = onClick, modifier = modifier)
}
