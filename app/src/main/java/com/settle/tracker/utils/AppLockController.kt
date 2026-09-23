package com.settle.tracker.utils

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Composition-wide biometric app-lock state – toggle from the Account screen,
 * re-lock from anywhere (e.g. on the app backgrounding) and the lock overlay
 * in MainActivity reacts.
 */
data class AppLockState(
    val enabled: MutableState<Boolean>,
    val unlocked: MutableState<Boolean>,
    private val prefs: SettlePrefs
) {
    fun setEnabled(value: Boolean) {
        enabled.value = value
        prefs.setBiometricLockEnabled(value)
        if (!value) unlocked.value = true
    }

    /** Re-lock, e.g. after the app comes back from the background. No-op if the feature is off. */
    fun lock() {
        if (enabled.value) unlocked.value = false
    }

    fun unlock() { unlocked.value = true }
}

val LocalAppLockState = staticCompositionLocalOf<AppLockState> {
    error("LocalAppLockState not provided")
}

@Composable
fun rememberAppLockState(context: Context): AppLockState {
    val prefs = remember { SettlePrefs(context.applicationContext) }
    val enabled = remember { mutableStateOf(prefs.isBiometricLockEnabled()) }
    val unlocked = remember { mutableStateOf(!enabled.value) }
    return remember(enabled, unlocked) { AppLockState(enabled, unlocked, prefs) }
}
