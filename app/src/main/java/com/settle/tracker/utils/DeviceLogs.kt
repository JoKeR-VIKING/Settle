package com.settle.tracker.utils

import android.os.Process
import java.io.BufferedReader
import java.io.InputStreamReader

private const val MAX_LOG_LINES = 300
private const val MAX_LOG_CHARS = 8000

/**
 * Lines that look like they carry a secret rather than a debugging fact —
 * stripped before a logcat capture ever leaves the device, on top of this
 * app never logging raw SMS bodies or account digits in the first place.
 */
private val REDACT_PATTERNS = listOf(
    // Redacts from the keyword to the end of the (single) line, not just the
    // keyword itself — the actual token always follows it on the same line.
    Regex("""(?i)\b(bearer|authorization)\b.*"""),
    Regex("""[A-Za-z0-9_-]{24,}\.[A-Za-z0-9_-]{6,}\.[A-Za-z0-9_-]{6,}"""), // JWT-shaped
    Regex("""\b[\w.+-]+@[\w.-]+\.[a-z]{2,}\b""", RegexOption.IGNORE_CASE) // email addresses
)

fun redactLogLine(line: String): String =
    REDACT_PATTERNS.fold(line) { acc, pattern -> pattern.replace(acc, "[redacted]") }

/**
 * The app's own recent logcat output. Android's logging daemon lets a
 * process read back only its own UID's log lines via the `logcat` binary —
 * no READ_LOGS permission (a system-only permission apps can't hold) and no
 * access to other apps' or the system's logs. Best-effort: returns null if
 * unavailable (OEM restriction, logcat missing, empty buffer).
 */
fun captureOwnProcessLogs(): String? = try {
    val pid = Process.myPid()
    val process = ProcessBuilder("logcat", "-d", "-v", "time", "--pid=$pid")
        .redirectErrorStream(true)
        .start()

    val lines = BufferedReader(InputStreamReader(process.inputStream)).readLines()
    process.waitFor()

    lines.takeLast(MAX_LOG_LINES)
        .joinToString("\n") { redactLogLine(it) }
        .take(MAX_LOG_CHARS)
        .ifBlank { null }
} catch (e: Exception) {
    null
}
