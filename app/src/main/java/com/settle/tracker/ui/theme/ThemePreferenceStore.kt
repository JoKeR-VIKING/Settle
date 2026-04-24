package com.settle.tracker.ui.theme

import android.content.Context

class ThemePreferenceStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun getDarkMode(): Boolean? {
        return if (prefs.contains(KEY_DARK_MODE)) prefs.getBoolean(KEY_DARK_MODE, false) else null
    }

    fun setDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
    }

    private companion object {
        const val PREFS_NAME = "settle_theme_prefs"
        const val KEY_DARK_MODE = "dark_mode_enabled"
    }
}
