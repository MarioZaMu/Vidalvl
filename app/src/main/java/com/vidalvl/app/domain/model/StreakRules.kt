package com.vidalvl.app.domain.model

object StreakRules {
    /**
     * Consecutive days with at least one completed mission. Today still counts as open:
     * if nothing was done yet today, the streak is the run that ends yesterday.
     * It is only a number; missing a day never removes XP, levels or items.
     */
    fun currentStreak(completionDays: Set<Long>, today: Long): Int {
        var day = if (today in completionDays) today else today - 1
        var streak = 0
        while (day in completionDays) {
            streak++
            day--
        }
        return streak
    }
}
