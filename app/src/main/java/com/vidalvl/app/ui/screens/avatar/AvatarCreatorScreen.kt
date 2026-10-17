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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.vidalvl.app.R
import com.vidalvl.app.domain.model.AvatarConfig
import com.vidalvl.app.domain.model.CatalogItem
import com.vidalvl.app.domain.model.Gender
import com.vidalvl.app.domain.model.ItemSlot
import com.vidalvl.app.ui.avatar.AvatarView
import com.vidalvl.app.ui.components.PixelCornerShape
import com.vidalvl.app.ui.components.PrimaryButton
import com.vidalvl.app.ui.components.SecondaryButton
import com.vidalvl.app.ui.components.pixelShadow

private val OptionShape = RoundedCornerShape(12.dp)

/**
 * Avatar creator. The preview stays pinned on top while the options scroll;
 * nothing is saved until the user taps Save.
 */
@Composable
fun AvatarCreatorScreen(
    initial: AvatarConfig,
    isEditing: Boolean,
    onSave: (AvatarConfig) -> Unit,
    onCancel: () -> Unit,
    equipment: Map<ItemSlot, CatalogItem> = emptyMap()
) {
    var gender by rememberSaveable { mutableStateOf(initial.gender) }
    var skinTone by rememberSaveable { mutableStateOf(initial.skinTone) }
    var hairStyle by rememberSaveable { mutableStateOf(initial.hairStyle) }
    var hairColor by rememberSaveable { mutableStateOf(initial.hairColor) }
    var eyeStyle by rememberSaveable { mutableStateOf(initial.eyeStyle) }
    var name by rememberSaveable { mutableStateOf(initial.name) }

    val draft = AvatarConfig(gender, skinTone, hairStyle, hairColor, eyeStyle, name)
    val previewDescription = stringResource(R.string.avatar_content_description)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(
                if (isEditing) R.string.avatar_edit_title else R.string.avatar_create_title
            ),
            style = MaterialTheme.typography.headlineLarge
        )

        val cardShape = remember { PixelCornerShape(unit = 3.dp, steps = 2) }
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .pixelShadow(cardShape, MaterialTheme.colorScheme.outline)
                .border(1.dp, MaterialTheme.colorScheme.outline, cardShape),
            shape = cardShape,
            color = MaterialTheme.colorScheme.surface
        ) {
            Box(modifier = Modifier.padding(12.dp), contentAlignment = Alignment.Center) {
                AvatarView(
                    config = draft,
                    equipment = equipment,
                    modifier = Modifier
                        .size(190.dp)
                        .semantics { contentDescription = previewDescription }
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(AvatarConfig.MAX_NAME_LENGTH) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.avatar_name)) },
                supportingText = {
                    Text(stringResource(R.string.avatar_name_counter, name.length, AvatarConfig.MAX_NAME_LENGTH))
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
            )

            OptionSection(stringResource(R.string.avatar_gender)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GenderOption(stringResource(R.string.avatar_gender_male), gender == Gender.MALE) {
                        gender = Gender.MALE
                    }
                    GenderOption(stringResource(R.string.avatar_gender_female), gender == Gender.FEMALE) {
                        gender = Gender.FEMALE
                    }
                }
            }

            OptionSection(stringResource(R.string.avatar_skin_tone)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (1..AvatarConfig.SKIN_TONES).forEach { tone ->
                        PreviewOption(
                            config = draft.copy(skinTone = tone),
                            description = stringResource(R.string.avatar_skin_tone_option, tone),
                            selected = skinTone == tone,
                            onClick = { skinTone = tone }
                        )
                    }
                }
            }

            OptionSection(stringResource(R.string.avatar_eyes)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    (1..AvatarConfig.EYE_STYLES).chunked(5).forEach { rowStyles ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowStyles.forEach { style ->
                                PreviewOption(
                                    config = draft.copy(eyeStyle = style),
                                    description = stringResource(R.string.avatar_eyes_option, style),
                                    selected = eyeStyle == style,
                                    headCrop = true,
                                    onClick = { eyeStyle = style }
                                )
                            }
                        }
                    }
                }
            }

            OptionSection(stringResource(R.string.avatar_hair_style)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    (1..AvatarConfig.HAIR_STYLES).chunked(3).forEach { rowStyles ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowStyles.forEach { style ->
                                PreviewOption(
                                    config = draft.copy(hairStyle = style),
                                    description = stringResource(R.string.avatar_hair_style_option, style),
                                    selected = hairStyle == style,
                                    headCrop = true,
                                    onClick = { hairStyle = style }
                                )
                            }
                        }
                    }
                }
            }

            OptionSection(stringResource(R.string.avatar_hair_color)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AvatarConfig.HAIR_COLORS.forEachIndexed { index, argb ->
                        ColorOption(
                            color = Color(argb),
                            description = stringResource(R.string.avatar_hair_color_option, index + 1),
                            selected = hairColor == argb,
                            onClick = { hairColor = argb }
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isEditing) {
                SecondaryButton(
                    text = stringResource(R.string.avatar_cancel),
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                )
            }
            PrimaryButton(
                text = stringResource(R.string.avatar_save),
                onClick = { onSave(draft.copy(name = name.trim())) },
                modifier = Modifier.weight(1f),
                enabled = name.isNotBlank()
            )
        }
    }
}

@Composable
private fun OptionSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        content()
    }
}

@Composable
private fun RowScope.GenderOption(label: String, selected: Boolean, onClick: () -> Unit) {
    val modifier = Modifier.weight(1f)
    if (selected) PrimaryButton(label, onClick = onClick, modifier = modifier)
    else SecondaryButton(label, onClick = onClick, modifier = modifier)
}

/** Selectable tile showing a small, still avatar so the user sees the result before choosing. */
@Composable
private fun RowScope.PreviewOption(
    config: AvatarConfig,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    headCrop: Boolean = false
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    Box(
        modifier = Modifier
            .weight(1f)
            .aspectRatio(1f)
            .clip(OptionShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(BorderStroke(if (selected) 3.dp else 1.dp, borderColor), OptionShape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = description }
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        AvatarView(config = config, animated = false, headCrop = headCrop, modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun RowScope.ColorOption(
    color: Color,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    Box(
        modifier = Modifier
            .weight(1f)
            .aspectRatio(1f)
            // Selectable first: the whole 48 dp cell is the touch target, not just the visible circle.
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = description }
            .padding(4.dp)
            .clip(CircleShape)
            .background(color)
            .border(BorderStroke(if (selected) 3.dp else 1.dp, borderColor), CircleShape)
    )
}
