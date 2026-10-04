package com.vidalvl.app.ui.theme

import android.content.Context

/** Saves the selected theme mode on the device. */
object ThemeStorage {
    private const val PREFS_NAME = "vidalvl_settings"
    private const val KEY_THEME_MODE = "theme_mode"

    fun load(context: Context): ThemeMode {
        val saved = context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_THEME_MODE, null)
        return ThemeMode.entries.firstOrNull { it.name == saved } ?: ThemeMode.SYSTEM
    }

    fun save(context: Context, mode: ThemeMode) {
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME_MODE, mode.name)
            .apply()
    }
}