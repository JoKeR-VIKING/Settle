package com.settle.tracker.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Composition-wide theme state holder – toggle from anywhere (e.g. Account
 * screen) and `SettleTheme` re-composes with the new value.
 */
data class ThemeState(
    val mode: MutableState<ThemeMode>
) {
    fun cycle() {
        mode.value = when (mode.value) {
            ThemeMode.SYSTEM -> ThemeMode.LIGHT
            ThemeMode.LIGHT  -> ThemeMode.DARK
            ThemeMode.DARK   -> ThemeMode.SYSTEM
        }
    }
    fun set(newMode: ThemeMode) { mode.value = newMode }
}

val LocalThemeState = staticCompositionLocalOf<ThemeState> {
    error("LocalThemeState not provided")
}

@Composable
fun rememberThemeState(context: Context): ThemeState {
    val prefs = remember { SettlePrefs(context.applicationContext) }
    val mode = remember { mutableStateOf(prefs.readThemeMode()) }
    LaunchedEffect(mode.value) {
        prefs.writeThemeMode(mode.value)
    }
    return remember(mode) { ThemeState(mode) }
}

/** Open a URL in an external browser. */
fun Context.openUrl(url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    } catch (_: Exception) {}
}

/** Open app info settings so user can grant a previously denied permission. */
fun Context.openAppSettings() {
    try {
        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.fromParts("package", packageName, null)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    } catch (_: Exception) {}
}

object SettleLinks {
    const val PRIVACY_POLICY =
        "https://docs.google.com/document/d/1Jak6toJ-i6BwRxzlvzp0pIldTsXKFGaSSMZRwxGjew4/edit"
}
