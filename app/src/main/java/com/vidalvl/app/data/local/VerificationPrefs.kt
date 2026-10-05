package com.vidalvl.app.data.local

import android.content.Context
import androidx.core.content.edit
import com.vidalvl.app.domain.verification.DailyUsageStore

/** Small settings for photo verification: privacy consent and today's check count. */
class VerificationPrefs(context: Context) : DailyUsageStore {
    private val prefs = context.applicationContext.getSharedPreferences("verification", Context.MODE_PRIVATE)

    var consentGiven: Boolean
        get() = prefs.getBoolean(KEY_CONSENT, false)
        set(value) = prefs.edit { putBoolean(KEY_CONSENT, value) }

    override fun usedOn(day: Long): Int =
        if (prefs.getLong(KEY_DAY, -1L) == day) prefs.getInt(KEY_COUNT, 0) else 0

    override fun recordUse(day: Long) {
        val count = usedOn(day) + 1
        prefs.edit {
            putLong(KEY_DAY, day)
            putInt(KEY_COUNT, count)
        }
    }

    private companion object {
        const val KEY_CONSENT = "consent_given"
        const val KEY_DAY = "usage_day"
        const val KEY_COUNT = "usage_count"
    }
}
