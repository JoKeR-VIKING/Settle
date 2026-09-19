package com.settle.tracker

import com.settle.tracker.utils.redactLogLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class DeviceLogsTest {
    @Test
    fun redactsBearerTokens() {
        val line = "D/Auth: refreshed Authorization: Bearer NOT-A-REAL-TOKEN-fixture-value"
        assertFalse(redactLogLine(line).contains("fixture-value"))
    }

    @Test
    fun redactsEmailAddresses() {
        val line = "I/Firestore: user prathamvasani1@gmail.com signed in"
        assertFalse(redactLogLine(line).contains("prathamvasani1@gmail.com"))
    }

    @Test
    fun leavesUnrelatedLinesUntouched() {
        val line = "E/Firestore: PERMISSION_DENIED: Missing or insufficient permissions."
        assertEquals(line, redactLogLine(line))
    }
}
