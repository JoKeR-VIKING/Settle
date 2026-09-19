package com.settle.tracker

import android.app.Application
import android.os.Process
import com.settle.tracker.utils.SettlePrefs
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess

private const val MAX_CRASH_CHARS = 6000

/**
 * Installs a crash handler that persists the last uncaught exception (class,
 * message, stack trace — never app data) across the process restart that
 * follows a crash, so a user can attach it next time they open Report
 * Issue. Always chains to the previous handler (Crashlytics installs its
 * own) so existing crash reporting and process-death behavior are
 * unaffected — this only adds a copy for the in-app report flow.
 */
class SettleApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val prefs = SettlePrefs(this)
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val writer = StringWriter()
                throwable.printStackTrace(PrintWriter(writer))
                prefs.writeLastCrash(writer.toString().take(MAX_CRASH_CHARS))
            } catch (_: Exception) {
                // Never let crash capture itself crash the crash handler.
            }

            if (previousHandler != null) {
                previousHandler.uncaughtException(thread, throwable)
            } else {
                Process.killProcess(Process.myPid())
                exitProcess(10)
            }
        }
    }
}
