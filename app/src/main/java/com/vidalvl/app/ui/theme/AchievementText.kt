package com.vidalvl.app.ui.theme

import androidx.annotation.StringRes
import com.vidalvl.app.R
import com.vidalvl.app.domain.model.Achievement

@StringRes
fun Achievement.titleRes(): Int = when (this) {
    Achievement.FIRST_STEP -> R.string.achievement_first_step_title
    Achievement.FIRST_WEEKLY -> R.string.achievement_first_weekly_title
    Achievement.PERFECT_DAY -> R.string.achievement_perfect_day_title
    Achievement.STREAK_3 -> R.string.achievement_streak_3_title
    Achievement.STREAK_7 -> R.string.achievement_streak_7_title
    Achievement.STREAK_14 -> R.string.achievement_streak_14_title
    Achievement.STREAK_30 -> R.string.achievement_streak_30_title
    Achievement.TOTAL_25 -> R.string.achievement_total_25_title
    Achievement.TOTAL_100 -> R.string.achievement_total_100_title
    Achievement.LEVEL_5 -> R.string.achievement_level_5_title
    Achievement.LEVEL_10 -> R.string.achievement_level_10_title
    Achievement.BALANCE -> R.string.achievement_balance_title
    Achievement.SPECIALIST -> R.string.achievement_specialist_title
    Achievement.COME_BACK -> R.string.achievement_come_back_title
    Achievement.COLLECTOR -> R.string.achievement_collector_title
}

@StringRes
fun Achievement.descriptionRes(): Int = when (this) {
    Achievement.FIRST_STEP -> R.string.achievement_first_step_desc
    Achievement.FIRST_WEEKLY -> R.string.achievement_first_weekly_desc
    Achievement.PERFECT_DAY -> R.string.achievement_perfect_day_desc
    Achievement.STREAK_3 -> R.string.achievement_streak_3_desc
    Achievement.STREAK_7 -> R.string.achievement_streak_7_desc
    Achievement.STREAK_14 -> R.string.achievement_streak_14_desc
    Achievement.STREAK_30 -> R.string.achievement_streak_30_desc
    Achievement.TOTAL_25 -> R.string.achievement_total_25_desc
    Achievement.TOTAL_100 -> R.string.achievement_total_100_desc
    Achievement.LEVEL_5 -> R.string.achievement_level_5_desc
    Achievement.LEVEL_10 -> R.string.achievement_level_10_desc
    Achievement.BALANCE -> R.string.achievement_balance_desc
    Achievement.SPECIALIST -> R.string.achievement_specialist_desc
    Achievement.COME_BACK -> R.string.achievement_come_back_desc
    Achievement.COLLECTOR -> R.string.achievement_collector_desc
}
