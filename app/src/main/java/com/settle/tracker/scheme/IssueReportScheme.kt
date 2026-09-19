package com.settle.tracker.scheme

import com.google.firebase.firestore.IgnoreExtraProperties
import com.settle.tracker.sms.SmsDiagnosticEntry

enum class IssueReportStatus {
    OPEN,
    RESOLVED
}

@IgnoreExtraProperties
data class IssueReportScheme(
    val id: String = "",
    val subject: String = "",
    val summary: String = "",
    val status: String = IssueReportStatus.OPEN.name,
    val reporterId: String = "",
    val reporterEmail: String = "",
    val appVersionName: String = "",
    val appVersionCode: Long = 0L,
    val androidVersion: String = "",
    val deviceModel: String = "",
    val diagnostics: List<SmsDiagnosticEntry> = emptyList(),
    val deviceLogs: String? = null,
    val crashInfo: String? = null,
    val createdAt: Long = 0L
)
