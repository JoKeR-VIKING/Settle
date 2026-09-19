package com.settle.tracker.sms

import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * A small in-memory (never persisted to disk) record of what SmsParse.parse
 * decided for a message and why — never the message body itself, and never
 * an account/card number. Exists so a user's "Report Issue" submission can
 * carry real signal about why an SMS wasn't booked, without the app ever
 * touching the raw bank SMS text outside the parsing pass that already
 * discards it.
 */
@IgnoreExtraProperties
data class SmsDiagnosticEntry(
    val timestamp: Long = 0L,
    val sender: String = "",
    val trust: String = "",
    val classification: String = "",
    val merchantFound: Boolean? = null,
    val paidFromFound: Boolean? = null,
    val outcome: String = ""
)

object SmsDiagnostics {
    private const val MAX_ENTRIES = 25
    private val buffer = ArrayDeque<SmsDiagnosticEntry>()

    @Synchronized
    fun record(
        sender: String,
        trust: String,
        classification: String,
        merchantFound: Boolean? = null,
        paidFromFound: Boolean? = null,
        outcome: String
    ) {
        if (buffer.size >= MAX_ENTRIES) buffer.removeFirst()
        buffer.addLast(
            SmsDiagnosticEntry(
                timestamp = System.currentTimeMillis(),
                sender = sender,
                trust = trust,
                classification = classification,
                merchantFound = merchantFound,
                paidFromFound = paidFromFound,
                outcome = outcome
            )
        )
    }

    /** Most recent first. */
    @Synchronized
    fun recent(): List<SmsDiagnosticEntry> = buffer.asReversed().toList()
}
