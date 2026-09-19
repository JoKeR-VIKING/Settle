package com.settle.tracker.screens

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Firebase
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.settle.tracker.scheme.IssueReportScheme
import com.settle.tracker.scheme.IssueReportStatus
import com.settle.tracker.utils.formatTimestamp

private enum class ReportSort { NEWEST, TITLE }

@Composable
fun IssueReportsScreen(
    onBack: () -> Unit
) {
    val db = Firebase.firestore

    var reports by remember { mutableStateOf<List<IssueReportScheme>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf(ReportSort.NEWEST) }
    var expandedId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        db.collection("issueReports")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                isLoading = false
                if (error != null) {
                    Log.e("Firestore", "Failed to load issue reports: ${error.message}")
                    return@addSnapshotListener
                }
                reports = snapshot?.documents?.mapNotNull { it.toObject(IssueReportScheme::class.java) }
                    ?: emptyList()
            }
    }

    val visible = remember(reports, query, sort) {
        val filtered = if (query.isBlank()) {
            reports
        } else {
            reports.filter { it.subject.contains(query, ignoreCase = true) }
        }
        when (sort) {
            ReportSort.NEWEST -> filtered.sortedByDescending { it.createdAt }
            ReportSort.TITLE -> filtered.sortedBy { it.subject.lowercase() }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    modifier = Modifier.size(25.dp),
                    imageVector = Icons.Filled.ChevronLeft,
                    contentDescription = "Go Back"
                )
            }
            Text(
                "Issue Reports (${reports.size})",
                style = MaterialTheme.typography.labelLarge,
                letterSpacing = 0.5.sp,
            )
        }

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(15),
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search by title") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) }
        )

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            listOf(ReportSort.NEWEST to "Newest", ReportSort.TITLE to "By Title")
                .forEachIndexed { index, (value, label) ->
                    SegmentedButton(
                        selected = sort == value,
                        onClick = { sort = value },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
        }

        if (!isLoading && visible.isEmpty()) {
            Text(
                if (reports.isEmpty()) "No issue reports yet." else "No reports match \"$query\".",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(visible, key = { it.id }) { report ->
                IssueReportCard(
                    report = report,
                    expanded = expandedId == report.id,
                    onToggleExpanded = {
                        expandedId = if (expandedId == report.id) null else report.id
                    },
                    onToggleStatus = {
                        val newStatus = if (report.status == IssueReportStatus.OPEN.name) {
                            IssueReportStatus.RESOLVED.name
                        } else {
                            IssueReportStatus.OPEN.name
                        }
                        db.collection("issueReports").document(report.id)
                            .update("status", newStatus)
                            .addOnFailureListener { e ->
                                Log.e("Firestore", "Failed to update report status: ${e.message}")
                            }
                    }
                )
            }
        }
    }
}

@Composable
private fun IssueReportCard(
    report: IssueReportScheme,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onToggleStatus: () -> Unit
) {
    val isOpen = report.status == IssueReportStatus.OPEN.name

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onToggleExpanded)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                report.subject.ifBlank { "(no subject)" },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            StatusChip(isOpen = isOpen)
        }

        Text(
            formatTimestamp(report.createdAt, "dd MMM YYYY"),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (!expanded) {
            Text(
                report.summary,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 2,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(report.summary, style = MaterialTheme.typography.labelMedium)

                Text(
                    "From: ${report.reporterEmail.ifBlank { report.reporterId }}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "App ${report.appVersionName} (${report.appVersionCode}) · " +
                        "Android ${report.androidVersion} · ${report.deviceModel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (report.diagnostics.isNotEmpty()) {
                    Text(
                        "SMS parsing diagnostics",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    report.diagnostics.forEach { entry ->
                        Text(
                            "${entry.sender} — ${entry.classification} — ${entry.outcome}" +
                                (entry.merchantFound?.let { " — merchant=${it}" } ?: "") +
                                (entry.paidFromFound?.let { " — paidFrom=${it}" } ?: ""),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                report.crashInfo?.let { crash ->
                    Text(
                        "Crash trace",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        crash,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                report.deviceLogs?.let { logs ->
                    Text(
                        "Device logs",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        logs,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedButton(onClick = onToggleStatus) {
                    Text(if (isOpen) "Mark Resolved" else "Reopen")
                }
            }
        }
    }
}

@Composable
private fun StatusChip(isOpen: Boolean) {
    Row(
        modifier = Modifier
            .background(
                if (isOpen) MaterialTheme.colorScheme.errorContainer
                else MaterialTheme.colorScheme.primaryContainer,
                RoundedCornerShape(50)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            if (isOpen) "Open" else "Resolved",
            style = MaterialTheme.typography.labelSmall,
            color = if (isOpen) MaterialTheme.colorScheme.onErrorContainer
            else MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
