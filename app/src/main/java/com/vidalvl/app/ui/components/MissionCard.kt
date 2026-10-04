package com.vidalvl.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

private const val RESTING_GLOW = 0.35f

/**
 * Mission card. The whole card is the button. The color strip on top shows the attribute.
 * When completed it pulses once and keeps a soft glow in the attribute color.
 */
@Composable
fun MissionCard(
    title: String,
    subtitle: String?,
    attributeLabel: String,
    attributeColor: Color,
    xp: Int,
    completed: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val glow = remember { Animatable(if (completed) RESTING_GLOW else 0f) }

    LaunchedEffect(completed) {
        if (completed) {
            // Pulse only when it was just completed (not when the screen reopens).
            if (glow.value < RESTING_GLOW) {
                glow.animateTo(1f, tween(300))
                glow.animateTo(RESTING_GLOW, tween(1000))
            }
        } else {
            glow.animateTo(0f, tween(250))
        }
    }

    val shape = RoundedCornerShape(14.dp)
    val glowValue = glow.value
    val colors = MaterialTheme.colorScheme

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = lerp(colors.surface, attributeColor, 0.2f * glowValue),
        border = BorderStroke(1.dp, lerp(colors.outline, attributeColor, glowValue))
    ) {
        Column(
            modifier = Modifier.clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onToggle()
            }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(attributeColor)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (completed) colors.onSurfaceVariant else colors.onSurface,
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
                        text = "$attributeLabel · +$xp XP",
                        style = MaterialTheme.typography.labelLarge,
                        color = attributeColor,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (completed) attributeColor else Color.Transparent)
                        .border(2.dp, attributeColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (completed) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Completada",
                            tint = colors.background,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}