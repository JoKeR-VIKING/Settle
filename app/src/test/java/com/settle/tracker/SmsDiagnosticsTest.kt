package com.settle.tracker

import com.settle.tracker.sms.SmsDiagnostics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsDiagnosticsTest {
    @Test
    fun mostRecentEntryComesFirst() {
        SmsDiagnostics.record("SENDER-A", "BANK", "Debit", merchantFound = true, paidFromFound = true, outcome = "booked")
        SmsDiagnostics.record("SENDER-B", "BANK", "NonFinancial", outcome = "not a completed debit")

        val recent = SmsDiagnostics.recent()
        assertTrue(recent.isNotEmpty())
        assertEquals("SENDER-B", recent.first().sender)
    }

    @Test
    fun bufferNeverExceedsCap() {
        repeat(40) { i ->
            SmsDiagnostics.record("SENDER-$i", "BANK", "Debit", outcome = "booked")
        }

        assertTrue(SmsDiagnostics.recent().size <= 25)
    }
}
