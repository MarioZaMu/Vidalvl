package com.vidalvl.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.vidalvl.app.R

private const val RESTING_GLOW = 0.35f

/**
 * Mission row. The whole row is the toggle. The badge on the left shows the attribute;
 * when completed it pulses once, keeps a soft glow in the attribute color and fades a little.
 */
@Composable
fun MissionCard(
    title: String,
    subtitle: String?,
    attributeLabel: String,
    attributeColor: Color,
    @DrawableRes iconRes: Int,
    xp: Int,
    completed: Boolean,
    onToggle: () -> Unit,
    onLongClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val glow = remember { Animatable(if (completed) RESTING_GLOW else 0f) }
    var burst by remember { mutableIntStateOf(0) }

    LaunchedEffect(completed) {
        if (completed) {
            // Pulse only when it was just completed (not when the screen reopens).
            if (glow.value < RESTING_GLOW) {
                burst++
                glow.animateTo(1f, tween(300))
                glow.animateTo(RESTING_GLOW, tween(1000))
            }
        } else {
            glow.animateTo(0f, tween(250))
        }
    }

    val shape = remember { PixelCornerShape(unit = 3.dp, steps = 2) }
    val checkShape = remember { PixelCornerShape(unit = 4.dp, steps = 2) }
    val glowValue = glow.value
    val colors = MaterialTheme.colorScheme
    val state = stringResource(if (completed) R.string.mission_completed else R.string.mission_pending)
    val editLabel = stringResource(R.string.action_edit)
    val deleteLabel = stringResource(R.string.action_delete)

    Surface(
        modifier = modifier.fillMaxWidth().pixelShadow(shape, colors.outline),
        shape = shape,
        color = lerp(colors.surface, attributeColor, 0.18f * glowValue),
        border = BorderStroke(1.dp, lerp(colors.outline, attributeColor, glowValue))
    ) {
        Row(
            modifier = Modifier
                .combinedClickable(
                    role = Role.Checkbox,
                    onLongClickLabel = editLabel,
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongClick()
                    },
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggle()
                    }
                )
                .semantics {
                    stateDescription = state
                    // TalkBack users get edit and delete without needing the long press.
                    customActions = listOf(
                        CustomAccessibilityAction(editLabel) { onEdit(); true },
                        CustomAccessibilityAction(deleteLabel) { onDelete(); true }
                    )
                }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(PixelCornerShape(unit = 3.dp, steps = 1))
                    .background(attributeColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = attributeColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f).alpha(if (completed) 0.65f else 1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                    textDecoration = if (completed) TextDecoration.LineThrough else null
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Text(
                    text = stringResource(R.string.mission_xp_attribute, attributeLabel, xp),
                    style = MaterialTheme.typography.labelLarge,
                    color = attributeColor,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Box(modifier = Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(checkShape)
                        .background(if (completed) attributeColor else Color.Transparent)
                        .border(2.dp, attributeColor, checkShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (completed) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = colors.background,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                PixelBurst(trigger = burst, color = attributeColor)
            }
        }
    }
}
