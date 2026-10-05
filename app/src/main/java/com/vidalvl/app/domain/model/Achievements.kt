package com.vidalvl.app.domain.model

/**
 * Achievements unlock by themselves from what the player does. Some give one of the special items that no
 * mission or level can give. Once unlocked they stay unlocked: nothing here is ever taken back.
 */
enum class Achievement(val rewardItemId: String? = null) {
    FIRST_STEP,
    FIRST_WEEKLY,
    PERFECT_DAY(rewardItemId = "hat_017_1"),
    STREAK_3,
    STREAK_7(rewardItemId = "hat_013"),
    STREAK_14(rewardItemId = "cloak_006"),
    STREAK_30(rewardItemId = "cloak_007"),
    TOTAL_25,
    TOTAL_100(rewardItemId = "hat_017_2"),
    LEVEL_5,
    LEVEL_10(rewardItemId = "hat_017_3"),
    BALANCE,
    SPECIALIST,
    COME_BACK,
    COLLECTOR
}

/** An achievement just unlocked, with the special item it gave (null if it gives none, or it was already owned). */
data class AchievementUnlock(val achievement: Achievement, val item: CatalogItem?)

/** Everything the rules need. Moments (perfect day, return) are only known when a mission is completed. */
data class AchievementStats(
    val totalCompletions: Int,
    val weeklyCompletions: Int,
    /** Longest run of consecutive days ever, so a past streak still counts after it was broken. */
    val bestStreak: Int,
    val totalLevel: Int,
    val attributeLevels: Map<Attribute, Int>,
    val itemCount: Int,
    /** True right after completing the last daily mission of a day (with at least three of them). */
    val perfectDay: Boolean = false,
    /** True right after completing a mission following three or more days without any. */
    val returnedAfterAbsence: Boolean = false
)

/** How far along a counted achievement is. Moment-based ones (perfect day, return) have none. */
data class AchievementProgress(val current: Int, val target: Int) {
    val fraction: Float get() = if (target == 0) 1f else (current.toFloat() / target).coerceIn(0f, 1f)
    val done: Boolean get() = current >= target
}

object AchievementRules {
    const val MIN_DAILY_FOR_PERFECT_DAY = 3
    const val ABSENCE_DAYS = 3
    const val BALANCE_LEVEL = 3
    const val SPECIALIST_LEVEL = 5
    const val COLLECTOR_ITEMS = 10

    fun progress(achievement: Achievement, stats: AchievementStats): AchievementProgress? = when (achievement) {
        Achievement.FIRST_STEP -> AchievementProgress(stats.totalCompletions, 1)
        Achievement.FIRST_WEEKLY -> AchievementProgress(stats.weeklyCompletions, 1)
        Achievement.STREAK_3 -> AchievementProgress(stats.bestStreak, 3)
        Achievement.STREAK_7 -> AchievementProgress(stats.bestStreak, 7)
        Achievement.STREAK_14 -> AchievementProgress(stats.bestStreak, 14)
        Achievement.STREAK_30 -> AchievementProgress(stats.bestStreak, 30)
        Achievement.TOTAL_25 -> AchievementProgress(stats.totalCompletions, 25)
        Achievement.TOTAL_100 -> AchievementProgress(stats.totalCompletions, 100)
        Achievement.LEVEL_5 -> AchievementProgress(stats.totalLevel, 5)
        Achievement.LEVEL_10 -> AchievementProgress(stats.totalLevel, 10)
        Achievement.BALANCE -> AchievementProgress(
            current = Attribute.entries.count { (stats.attributeLevels[it] ?: 1) >= BALANCE_LEVEL },
            target = Attribute.entries.size
        )
        Achievement.SPECIALIST -> AchievementProgress(
            current = stats.attributeLevels.values.maxOrNull() ?: 1,
            target = SPECIALIST_LEVEL
        )
        Achievement.COLLECTOR -> AchievementProgress(stats.itemCount, COLLECTOR_ITEMS)
        Achievement.PERFECT_DAY, Achievement.COME_BACK -> null
    }

    /** Whether [achievement] is earned by [stats]. */
    fun isEarned(achievement: Achievement, stats: AchievementStats): Boolean = when (achievement) {
        Achievement.PERFECT_DAY -> stats.perfectDay
        Achievement.COME_BACK -> stats.returnedAfterAbsence
        else -> progress(achievement, stats)?.done ?: false
    }

    /** Achievements earned by [stats] that are not in [alreadyUnlocked], in catalog order. */
    fun newlyEarned(stats: AchievementStats, alreadyUnlocked: Set<Achievement>): List<Achievement> =
        Achievement.entries.filter { it !in alreadyUnlocked && isEarned(it, stats) }

    /** The achievement that gives [itemId], if any. */
    fun givingItem(itemId: String): Achievement? = Achievement.entries.firstOrNull { it.rewardItemId == itemId }
}
