package com.settle.tracker.utils

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Ultra-light sound & haptics manager. Uses the built-in ToneGenerator so no
 * asset/file is needed. Must be created ONCE per app (it allocates an audio
 * system resource). Share via [LocalSoundManager].
 */
class SoundManager(context: Context) {
    private val tone: ToneGenerator? = try {
        ToneGenerator(AudioManager.STREAM_MUSIC, 30)
    } catch (e: Exception) {
        null
    }

    private val vibrator: Vibrator? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    } catch (_: Exception) { null }

    fun tap() { safeTone(ToneGenerator.TONE_PROP_PROMPT, 40); vibrate(8) }
    fun success() { safeTone(ToneGenerator.TONE_PROP_ACK, 120); vibrate(20) }
    fun error() { safeTone(ToneGenerator.TONE_PROP_NACK, 150); vibrate(40) }
    fun delete() { safeTone(ToneGenerator.TONE_PROP_BEEP2, 80); vibrate(12) }

    private fun safeTone(type: Int, ms: Int) {
        try { tone?.startTone(type, ms) } catch (_: Exception) {}
    }

    private fun vibrate(ms: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(ms)
            }
        } catch (_: Exception) {}
    }

    fun release() = try { tone?.release() } catch (_: Exception) {}
}

/**
 * Use this CompositionLocal everywhere – avoids allocating a ToneGenerator per
 * row in LazyColumns (which was making scrolling extremely laggy).
 */
val LocalSoundManager = staticCompositionLocalOf<SoundManager> {
    error("LocalSoundManager not provided. Wrap content in CompositionLocalProvider.")
}

/** Shorthand reader. */
@Composable
fun rememberSoundManager(): SoundManager = LocalSoundManager.current

