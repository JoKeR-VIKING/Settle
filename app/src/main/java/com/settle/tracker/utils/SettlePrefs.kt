package com.settle.tracker.utils

import android.content.Context
import androidx.core.content.edit

/**
 * Tiny wrapper over SharedPreferences for first-run / tutorial flags.
 */
class SettlePrefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("settle_prefs", Context.MODE_PRIVATE)

    fun isFirstRun(key: String): Boolean = sp.getBoolean("first_run_$key", true)

    fun markSeen(key: String) {
        sp.edit { putBoolean("first_run_$key", false) }
    }

    fun isSoundEnabled(): Boolean = sp.getBoolean("sound_enabled", true)

    fun setSoundEnabled(enabled: Boolean) {
        sp.edit { putBoolean("sound_enabled", enabled) }
    }

    companion object {
        const val TUTORIAL_EXPENSES = "tutorial_expenses"
        const val TUTORIAL_GROUPS = "tutorial_groups"
        const val PROMPT_NOTIFICATIONS = "prompt_notifications"
    }
}
