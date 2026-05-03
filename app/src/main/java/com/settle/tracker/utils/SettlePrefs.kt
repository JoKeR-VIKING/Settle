package com.settle.tracker.utils

import android.content.Context
import androidx.core.content.edit

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * Tiny wrapper over SharedPreferences for first-run flags and user preferences.
 */
class SettlePrefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("settle_prefs", Context.MODE_PRIVATE)

    fun isFirstRun(key: String): Boolean = sp.getBoolean("first_run_$key", true)
    fun markSeen(key: String) { sp.edit { putBoolean("first_run_$key", false) } }

    fun allToursSeen(): Boolean {
        return !isFirstRun(TUTORIAL_EXPENSES) &&
               !isFirstRun(TUTORIAL_GROUPS) &&
               !isFirstRun(TUTORIAL_ANALYTICS)
    }

    fun resetTours() {
        sp.edit {
            putBoolean("first_run_$TUTORIAL_EXPENSES", true)
            putBoolean("first_run_$TUTORIAL_GROUPS", true)
            putBoolean("first_run_$TUTORIAL_ANALYTICS", true)
        }
    }

    fun readThemeMode(): ThemeMode {
        val raw = sp.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        return runCatching { ThemeMode.valueOf(raw) }.getOrDefault(ThemeMode.SYSTEM)
    }
    fun writeThemeMode(mode: ThemeMode) { sp.edit { putString("theme_mode", mode.name) } }

    companion object {
        const val TUTORIAL_EXPENSES = "tutorial_expenses"
        const val TUTORIAL_GROUPS = "tutorial_groups"
        const val TUTORIAL_ANALYTICS = "tutorial_analytics"
        const val PROMPT_INITIAL_PERMISSIONS = "prompt_initial_permissions"
        const val PROMPT_NOTIFICATIONS = "prompt_notifications"
    }
}
