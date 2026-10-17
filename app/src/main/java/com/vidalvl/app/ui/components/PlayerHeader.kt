package com.vidalvl.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vidalvl.app.R
import com.vidalvl.app.ui.theme.VidalvlTheme

/**
 * Player card for the top of the missions screen: avatar, name, level, XP bar,
 * today's progress ring and three small stats.
 */
@Composable
fun PlayerHeader(
    name: String,
    level: Int,
    xpIntoLevel: Int,
    xpForNextLevel: Int,
    levelProgress: Float,
    dailyDone: Int,
    dailyTotal: Int,
    streak: Int,
    totalXp: Int,
    itemCount: Int,
    itemTotal: Int,
    modifier: Modifier = Modifier,
    avatar: @Composable () -> Unit
) {
    val shape = PixelCornerShape(unit = 3.dp, steps = 2)
    Surface(
        modifier = modifier.fillMaxWidth().pixelShadow(shape, MaterialTheme.colorScheme.outline),
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Name, level and XP bar. With a large system font there is no room for them between the avatar
            // and the ring, so they move below and take the full width.
            val info: @Composable (Modifier) -> Unit = { infoModifier ->
                Column(modifier = infoModifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LevelPill(level)
                        Text(
                            text = name.ifBlank { stringResource(R.string.player_default_name) },
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    StatBar(
                        label = stringResource(R.string.player_xp_label),
                        valueText = stringResource(R.string.player_xp_value, xpIntoLevel, xpForNextLevel),
                        progress = levelProgress,
                        color = VidalvlTheme.attributeColors.xp
                    )
                }
            }

            if (LocalDensity.current.fontScale >= LARGE_FONT_SCALE) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AvatarFrame(avatar)
                    DayRing(done = dailyDone, total = dailyTotal)
                }
                info(Modifier.fillMaxWidth())
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AvatarFrame(avatar)
                    info(Modifier.weight(1f))
                    DayRing(done = dailyDone, total = dailyTotal)
                }
            }

            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatTile(stringResource(R.string.stat_streak_value, streak), stringResource(R.string.stat_streak))
                StatTile(totalXp.toString(), stringResource(R.string.stat_total_xp))
                StatTile(stringResource(R.string.stat_items_value, itemCount, itemTotal), stringResource(R.string.stat_items))
            }
        }
    }
}

/** From this system font scale up, layouts that are tight at normal size switch to a roomier arrangement. */
private const val LARGE_FONT_SCALE = 1.3f

@Composable
private fun AvatarFrame(avatar: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(76.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) { avatar() }
}

@Composable
fun LevelPill(level: Int) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Text(
            text = stringResource(R.string.player_level, level),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

/** Ring with today's completed daily missions. */
@Composable
private fun DayRing(done: Int, total: Int) {
    val progress = if (total == 0) 0f else done.toFloat() / total
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "dayRing"
    )
    val track = MaterialTheme.colorScheme.outline
    val fill = MaterialTheme.colorScheme.primary
    val description = stringResource(R.string.player_today_description, done, total)

    Box(
        modifier = Modifier
            .size(60.dp)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(60.dp)) {
            val stroke = 6.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = track, startAngle = 0f, sweepAngle = 360f, useCenter = false,
                topLeft = Offset(inset, inset), size = arcSize, style = Stroke(stroke)
            )
            drawArc(
                color = fill, startAngle = -90f, sweepAngle = 360f * animated, useCenter = false,
                topLeft = Offset(inset, inset), size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
        CappedFontScale(max = 1.15f) {
            Column(
                modifier = Modifier.clearAndSetSemantics { },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("$done/$total", style = MaterialTheme.typography.labelLarge)
                Text(
                    stringResource(R.string.player_today),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun RowScope.StatTile(value: String, label: String) {
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, style = MaterialTheme.typography.titleMedium, maxLines = 1)
        CappedFontScale(max = 1.6f) {
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}
