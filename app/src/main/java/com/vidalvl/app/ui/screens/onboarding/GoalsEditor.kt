package com.vidalvl.app.ui.screens.onboarding

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.vidalvl.app.R
import com.vidalvl.app.domain.model.Goal
import com.vidalvl.app.domain.model.GoalRules
import com.vidalvl.app.domain.model.PlayerGoals
import com.vidalvl.app.ui.components.PrimaryButton
import com.vidalvl.app.ui.components.SecondaryButton
import com.vidalvl.app.ui.theme.VidalvlTheme
import com.vidalvl.app.ui.theme.forAttribute
import com.vidalvl.app.ui.theme.iconRes

@StringRes
internal fun Goal.titleRes(): Int = when (this) {
    Goal.FIT -> R.string.goal_fit
    Goal.LEARN -> R.string.goal_learn
    Goal.DISCIPLINE -> R.string.goal_discipline
    Goal.RELATIONSHIPS -> R.string.goal_relationships
    Goal.SLEEP -> R.string.goal_sleep
    Goal.LESS_PHONE -> R.string.goal_less_phone
    Goal.EAT_WELL -> R.string.goal_eat_well
    Goal.STRESS -> R.string.goal_stress
}

@StringRes
internal fun minutesRes(minutes: Int): Int = when (minutes) {
    5 -> R.string.time_5
    15 -> R.string.time_15
    else -> R.string.time_30
}

/** Up to [GoalRules.MAX_GOALS] goals. Once full, the rest dim until the user unselects one. */
@Composable
fun GoalPicker(selected: Set<Goal>, onToggle: (Goal) -> Unit) {
    val colors = VidalvlTheme.attributeColors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Goal.entries.forEach { goal ->
            val isSelected = goal in selected
            val enabled = isSelected || selected.size < GoalRules.MAX_GOALS
            val color = colors.forAttribute(goal.attribute)
            OptionRow(
                title = stringResource(goal.titleRes()),
                iconRes = goal.attribute.iconRes(),
                accent = color,
                selected = isSelected,
                enabled = enabled,
                modifier = Modifier.toggleable(
                    value = isSelected,
                    enabled = enabled,
                    role = Role.Checkbox,
                    onValueChange = { onToggle(goal) }
                )
            )
        }
    }
}

/** One time option at a time. */
@Composable
fun TimePicker(selected: Int, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        GoalRules.MINUTE_OPTIONS.forEach { minutes ->
            OptionRow(
                title = stringResource(minutesRes(minutes)),
                iconRes = null,
                accent = MaterialTheme.colorScheme.primary,
                selected = minutes == selected,
                enabled = true,
                modifier = Modifier.selectable(
                    selected = minutes == selected,
                    role = Role.RadioButton,
                    onClick = { onSelect(minutes) }
                )
            )
        }
    }
}

@Composable
private fun OptionRow(
    title: String,
    @DrawableRes iconRes: Int?,
    accent: Color,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier
) {
    val shape = RoundedCornerShape(14.dp)
    val border = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(BorderStroke(if (selected) 3.dp else 1.dp, border), shape)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (iconRes != null) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(painterResource(iconRes), contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            }
        }
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        if (selected) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/** Changes goals and time after the welcome screen (opened from the profile). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsEditorSheet(
    initial: PlayerGoals?,
    onDismiss: () -> Unit,
    onSave: (PlayerGoals) -> Unit
) {
    var goals by rememberSaveable { mutableStateOf<Set<Goal>>(initial?.goals ?: emptySet()) }
    var minutes by rememberSaveable { mutableIntStateOf(initial?.dailyMinutes ?: GoalRules.DEFAULT_MINUTES) }
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
                stringResource(R.string.profile_goals_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() }
            )
            Text(stringResource(R.string.onboarding_goals_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.goals_counter, goals.size, GoalRules.MAX_GOALS),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            GoalPicker(goals) { goals = GoalRules.toggle(goals, it) }
            Text(stringResource(R.string.onboarding_time_title), style = MaterialTheme.typography.titleMedium)
            TimePicker(minutes) { minutes = it }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryButton(
                    text = stringResource(R.string.action_cancel),
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
                PrimaryButton(
                    text = stringResource(R.string.action_save),
                    onClick = { onSave(PlayerGoals(goals, minutes)) },
                    modifier = Modifier.weight(1f),
                    enabled = GoalRules.canContinue(goals)
                )
            }
        }
    }
}
