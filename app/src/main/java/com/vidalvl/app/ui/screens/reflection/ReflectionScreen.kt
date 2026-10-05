package com.vidalvl.app.ui.screens.reflection

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vidalvl.app.R
import com.vidalvl.app.domain.model.DayCell
import com.vidalvl.app.domain.model.DayGroup
import com.vidalvl.app.domain.model.ItemCatalog
import com.vidalvl.app.domain.model.TimelineEvent
import com.vidalvl.app.domain.model.WeekSummary
import com.vidalvl.app.ui.avatar.ItemSprite
import com.vidalvl.app.ui.components.SegmentedTabs
import com.vidalvl.app.ui.components.StatTile
import com.vidalvl.app.ui.theme.VidalvlTheme
import com.vidalvl.app.ui.theme.descriptionRes
import com.vidalvl.app.ui.theme.forAttribute
import com.vidalvl.app.ui.theme.iconRes
import com.vidalvl.app.ui.theme.currentLocale
import com.vidalvl.app.ui.theme.label
import com.vidalvl.app.ui.theme.titleRes
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

/** "Reflejo": how the week went, everything that happened so far, and the achievements. */
@Composable
fun ReflectionScreen(viewModel: ReflectionViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val zone = ZoneId.systemDefault()

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "title") {
            Text(
                stringResource(R.string.reflection_title),
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.semantics { heading() }
            )
        }
        item(key = "week") { WeekCard(state.week) }
        item(key = "tabs") {
            SegmentedTabs(
                labels = listOf(
                    stringResource(R.string.reflection_tab_timeline),
                    stringResource(R.string.reflection_tab_achievements)
                ),
                selectedIndex = tab,
                onSelect = { tab = it }
            )
        }

        if (tab == 0) {
            if (state.timeline.isEmpty()) {
                item(key = "empty") { EmptyMessage(stringResource(R.string.timeline_empty)) }
            }
            state.timeline.forEach { group ->
                item(key = "day_${group.epochDay}") { DayHeader(group) }
                items(group.events) { event -> EventRow(event, zone) }
            }
        } else {
            item(key = "counter") {
                Text(
                    stringResource(R.string.achievements_counter, state.unlockedCount, state.achievements.size),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(state.achievements, key = { it.achievement.name }) { row -> AchievementCard(row, zone) }
        }
    }
}

@Composable
private fun WeekCard(week: WeekSummary) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(stringResource(R.string.reflection_week_title), style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                week.days.forEach { DayDot(it) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile(week.completionsThisWeek.toString(), stringResource(R.string.reflection_stat_week))
                StatTile(
                    stringResource(R.string.stat_streak_value, week.bestStreak),
                    stringResource(R.string.reflection_stat_best_streak)
                )
                StatTile(
                    week.topAttribute?.label() ?: stringResource(R.string.reflection_none),
                    stringResource(R.string.reflection_stat_top)
                )
            }
        }
    }
}

@Composable
private fun DayDot(cell: DayCell) {
    val locale = currentLocale()
    val weekday = LocalDate.ofEpochDay(cell.epochDay).dayOfWeek
    val initial = weekday.getDisplayName(TextStyle.NARROW, locale)
    val fullName = weekday.getDisplayName(TextStyle.FULL, locale)
    val description = stringResource(
        if (cell.hasActivity) R.string.reflection_day_active else R.string.reflection_day_idle,
        fullName
    )
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .alpha(if (cell.isFuture) 0.45f else 1f)
            .semantics(mergeDescendants = true) { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(initial, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(if (cell.hasActivity) colors.primary else colors.surfaceVariant)
                .border(
                    width = if (cell.isToday) 2.dp else 1.dp,
                    color = if (cell.isToday) colors.primary else colors.outline,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (cell.hasActivity) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun EmptyMessage(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 16.dp)
    )
}

@Composable
private fun DayHeader(group: DayGroup) {
    val locale = currentLocale()
    val today = LocalDate.now().toEpochDay()
    val label = when (group.epochDay) {
        today -> stringResource(R.string.reflection_today)
        today - 1 -> stringResource(R.string.reflection_yesterday)
        else -> DateTimeFormatter.ofPattern("EEEE d MMM", locale)
            .format(LocalDate.ofEpochDay(group.epochDay))
            .replaceFirstChar { it.titlecase(locale) }
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp).semantics { heading() }
    )
}

@Composable
private fun EventRow(event: TimelineEvent, zone: ZoneId) {
    val colors = VidalvlTheme.attributeColors
    val time = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
        .withLocale(currentLocale()).withZone(zone)
        .format(Instant.ofEpochMilli(event.millis))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when (event) {
            is TimelineEvent.MissionDone -> {
                val color = colors.forAttribute(event.attribute)
                Badge(color) { Icon(painterResource(event.attribute.iconRes()), null, tint = color, modifier = Modifier.size(22.dp)) }
                EventText(
                    title = event.title,
                    subtitle = stringResource(R.string.mission_xp_attribute, event.attribute.label(), event.xp),
                    modifier = Modifier.weight(1f)
                )
            }

            is TimelineEvent.ItemEarned -> {
                val item = ItemCatalog.byId(event.itemId)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) { if (item != null) ItemSprite(item, Modifier.size(36.dp)) }
                EventText(
                    title = stringResource(R.string.timeline_item, item?.let { stringResource(it.nameRes) }.orEmpty()),
                    subtitle = null,
                    modifier = Modifier.weight(1f)
                )
            }

            is TimelineEvent.AchievementUnlocked -> {
                val color = VidalvlTheme.attributeColors.accentText
                Badge(color) { Icon(painterResource(R.drawable.ic_trophy), null, tint = color, modifier = Modifier.size(22.dp)) }
                EventText(
                    title = stringResource(R.string.item_req_achievement, stringResource(event.achievement.titleRes())),
                    subtitle = stringResource(event.achievement.descriptionRes()),
                    modifier = Modifier.weight(1f)
                )
            }

            is TimelineEvent.LevelUp -> {
                val color = colors.xp
                Badge(color) { Icon(painterResource(R.drawable.ic_attr_social), null, tint = color, modifier = Modifier.size(22.dp)) }
                EventText(
                    title = stringResource(R.string.timeline_level_up, event.level),
                    subtitle = null,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Text(time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Badge(color: androidx.compose.ui.graphics.Color, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
private fun EventText(title: String, subtitle: String?, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AchievementCard(row: AchievementRowUi, zone: ZoneId) {
    val colors = MaterialTheme.colorScheme
    val prize = row.achievement.rewardItemId?.let { ItemCatalog.byId(it) }
    val locale = currentLocale()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = colors.surface,
        border = BorderStroke(if (row.unlocked) 2.dp else 1.dp, if (row.unlocked) colors.primary else colors.outline)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (row.unlocked) colors.primary.copy(alpha = 0.2f) else colors.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_trophy),
                    contentDescription = null,
                    tint = if (row.unlocked) VidalvlTheme.attributeColors.accentText else colors.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(26.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f).alpha(if (row.unlocked) 1f else 0.85f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(stringResource(row.achievement.titleRes()), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(row.achievement.descriptionRes()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
                val progress = row.progress
                if (!row.unlocked && progress != null) {
                    LinearProgressIndicator(
                        progress = { progress.fraction },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    )
                    Text(
                        stringResource(R.string.achievement_progress, progress.current.coerceAtMost(progress.target), progress.target),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant
                    )
                }
                row.unlockedAtMillis?.let { millis ->
                    val date = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                        .withLocale(locale).withZone(zone).format(Instant.ofEpochMilli(millis))
                    Text(
                        stringResource(R.string.achievement_unlocked_on, date),
                        style = MaterialTheme.typography.labelSmall,
                        color = VidalvlTheme.attributeColors.accentText
                    )
                }
                if (prize != null) {
                    Row(
                        modifier = Modifier.padding(top = 4.dp).alpha(if (row.unlocked) 1f else 0.5f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ItemSprite(prize, Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(colors.surfaceVariant))
                        Text(
                            stringResource(R.string.achievement_prize, stringResource(prize.nameRes)),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
