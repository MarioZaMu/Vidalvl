package com.vidalvl.app.domain.model

/** Level derived from raw XP. */
data class LevelInfo(
    val level: Int,
    val xpIntoLevel: Int,
    val xpForNextLevel: Int
) {
    val progress: Float
        get() = xpIntoLevel.toFloat() / xpForNextLevel
}

object GameRules {
    private const val BASE_XP_PER_LEVEL = 100
    private const val EXTRA_XP_PER_LEVEL = 20
    private const val GRACE_DAYS = 1
    private const val DAYS_TO_MIN_GLOW = 7
    private const val MIN_GLOW = 0.3f
    private const val NEUTRAL_GLOW = 0.6f

    private const val DAILY_MISSION_XP = 8
    private const val WEEKLY_MISSION_XP = 15

    /** XP is set by the mission type, never by the user: the app measures consistency, not effort inflation. */
    fun missionXp(type: MissionType): Int = when (type) {
        MissionType.DAILY -> DAILY_MISSION_XP
        MissionType.WEEKLY -> WEEKLY_MISSION_XP
    }

    /** XP needed to go from [level] to the next one: 100, 120, 140... */
    fun xpForLevel(level: Int): Int = BASE_XP_PER_LEVEL + EXTRA_XP_PER_LEVEL * (level - 1)

    fun levelFor(totalXp: Int): LevelInfo {
        var level = 1
        var remaining = totalXp.coerceAtLeast(0)
        var needed = xpForLevel(level)
        while (remaining >= needed) {
            remaining -= needed
            level++
            needed = xpForLevel(level)
        }
        return LevelInfo(level, remaining, needed)
    }

    /**
     * Glow from 1.0 (full) down to MIN_GLOW. Missed days only dim the creature:
     * XP and levels are never lost. A never-trained attribute has a neutral glow.
     */
    fun glowFor(lastCompletedDayEpoch: Long?, todayEpoch: Long): Float {
        if (lastCompletedDayEpoch == null) return NEUTRAL_GLOW
        val missedDays = (todayEpoch - lastCompletedDayEpoch - GRACE_DAYS).coerceAtLeast(0)
        val dimming = (missedDays.toFloat() / DAYS_TO_MIN_GLOW).coerceIn(0f, 1f)
        return 1f - dimming * (1f - MIN_GLOW)
    }
}