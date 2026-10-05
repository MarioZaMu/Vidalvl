package com.vidalvl.app.domain.model

import java.time.Instant
import java.time.ZoneId

/** Something worth remembering, shown in the "Reflejo" timeline. */
sealed interface TimelineEvent {
    val millis: Long

    data class MissionDone(
        override val millis: Long,
        val title: String,
        val attribute: Attribute,
        val xp: Int
    ) : TimelineEvent

    data class ItemEarned(override val millis: Long, val itemId: String) : TimelineEvent
    data class AchievementUnlocked(override val millis: Long, val achievement: Achievement) : TimelineEvent
    data class LevelUp(override val millis: Long, val level: Int) : TimelineEvent
}

data class DayGroup(val epochDay: Long, val events: List<TimelineEvent>)

data class CompletionFact(
    val millis: Long,
    val day: Long,
    val title: String,
    val attribute: Attribute,
    val xp: Int,
    val type: MissionType
)

data class ItemFact(val itemId: String, val millis: Long)
data class AchievementFact(val achievement: Achievement, val millis: Long)

data class DayCell(val epochDay: Long, val hasActivity: Boolean, val isToday: Boolean, val isFuture: Boolean)

data class WeekSummary(
    /** Monday to Sunday of the current week. */
    val days: List<DayCell>,
    val completionsThisWeek: Int,
    val bestStreak: Int,
    /** The attribute with the most completions so far; null before the first one. */
    val topAttribute: Attribute?
)

object ReflectionRules {
    const val MAX_EVENTS = 300

    /**
     * Groups the player's history by day, newest first. Level-ups are not stored: they are worked out by adding up
     * the XP of the completions in order, so the timeline always agrees with the history it shows.
     */
    fun timeline(
        completions: List<CompletionFact>,
        items: List<ItemFact>,
        achievements: List<AchievementFact>,
        zone: ZoneId,
        limit: Int = MAX_EVENTS
    ): List<DayGroup> {
        val events = mutableListOf<TimelineEvent>()

        var totalXp = 0
        completions.sortedBy { it.millis }.forEach { fact ->
            val before = GameRules.levelFor(totalXp).level
            totalXp += fact.xp
            val after = GameRules.levelFor(totalXp).level
            events += TimelineEvent.MissionDone(fact.millis, fact.title, fact.attribute, fact.xp)
            if (after > before) events += TimelineEvent.LevelUp(fact.millis, after)
        }
        items.forEach { events += TimelineEvent.ItemEarned(it.millis, it.itemId) }
        achievements.forEach { events += TimelineEvent.AchievementUnlocked(it.millis, it.achievement) }

        // Newest first; at the same instant the consequence (level up, achievement, item) comes before its cause.
        val ordered = events
            .sortedWith(compareByDescending<TimelineEvent> { it.millis }.thenBy { priority(it) })
            .take(limit)

        return ordered
            .groupBy { Instant.ofEpochMilli(it.millis).atZone(zone).toLocalDate().toEpochDay() }
            .map { (day, dayEvents) -> DayGroup(day, dayEvents) }
            .sortedByDescending { it.epochDay }
    }

    private fun priority(event: TimelineEvent): Int = when (event) {
        is TimelineEvent.LevelUp -> 0
        is TimelineEvent.AchievementUnlocked -> 1
        is TimelineEvent.ItemEarned -> 2
        is TimelineEvent.MissionDone -> 3
    }

    fun weekSummary(completions: List<CompletionFact>, today: Long): WeekSummary {
        val monday = today - (java.time.LocalDate.ofEpochDay(today).dayOfWeek.value - 1)
        val activeDays = completions.map { it.day }.toSet()
        return WeekSummary(
            days = (0..6).map { offset ->
                val day = monday + offset
                DayCell(day, hasActivity = day in activeDays, isToday = day == today, isFuture = day > today)
            },
            completionsThisWeek = completions.count { it.day in monday..(monday + 6) },
            bestStreak = StreakRules.longestStreak(activeDays),
            topAttribute = completions.groupingBy { it.attribute }.eachCount()
                .entries.maxWithOrNull(compareBy<Map.Entry<Attribute, Int>> { it.value }.thenByDescending { it.key.ordinal })
                ?.key
        )
    }
}
