package com.vidalvl.app.domain.model

object StreakRules {
    /** The longest run of consecutive days in [completionDays], whenever it happened. */
    fun longestStreak(completionDays: Set<Long>): Int {
        var best = 0
        for (day in completionDays) {
            if ((day - 1) in completionDays) continue // not the start of a run
            var length = 1
            while ((day + length) in completionDays) length++
            if (length > best) best = length
        }
        return best
    }

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
