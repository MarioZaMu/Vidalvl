package com.vidalvl.app.data.local

import android.content.Context
import androidx.core.content.edit
import com.vidalvl.app.domain.suggestions.SuggestionSettings

/** Whether suggestions use the AI (null = not asked yet) and when they were last made. */
class SuggestionPrefs(context: Context) : SuggestionSettings {
    private val prefs = context.applicationContext.getSharedPreferences("suggestions", Context.MODE_PRIVATE)

    override var aiEnabled: Boolean?
        get() = if (prefs.contains(KEY_AI)) prefs.getBoolean(KEY_AI, false) else null
        set(value) = prefs.edit {
            if (value == null) remove(KEY_AI) else putBoolean(KEY_AI, value)
        }

    override var lastDailyDay: Long
        get() = prefs.getLong(KEY_DAILY, -1L)
        set(value) = prefs.edit { putLong(KEY_DAILY, value) }

    override var lastWeeklyMonday: Long
        get() = prefs.getLong(KEY_WEEKLY, -1L)
        set(value) = prefs.edit { putLong(KEY_WEEKLY, value) }

    private companion object {
        const val KEY_AI = "ai_enabled"
        const val KEY_DAILY = "last_daily_day"
        const val KEY_WEEKLY = "last_weekly_monday"
    }
}
