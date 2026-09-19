package com.settle.tracker.screens

import android.os.Build
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.firestore
import com.settle.tracker.BuildConfig
import com.settle.tracker.components.LoadingScreenWrapper
import com.settle.tracker.components.common.SuccessOverlay
import com.settle.tracker.scheme.IssueReportScheme
import com.settle.tracker.sms.SmsDiagnosticEntry
import com.settle.tracker.sms.SmsDiagnostics
import com.settle.tracker.utils.mentionsSmsIssue
import java.util.UUID

private const val MAX_SUBJECT_CHARS = 80
private const val MAX_SUMMARY_CHARS = 600

@Composable
fun ReportIssueScreen(
    onBack: () -> Unit,
    currentUser: FirebaseUser
) {
    val db = Firebase.firestore

    var subject by remember { mutableStateOf(TextFieldValue("")) }
    var summary by remember { mutableStateOf(TextFieldValue("")) }
    var includeDiagnostics by remember { mutableStateOf(true) }
    var isSubmitting by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }

    val diagnosticsRelevant = mentionsSmsIssue(subject.text, summary.text)
    val diagnosticsAvailable = remember(diagnosticsRelevant) {
        if (diagnosticsRelevant) SmsDiagnostics.recent() else emptyList()
    }

    fun submit() {
        isSubmitting = true

        val id = UUID.randomUUID().toString()
        val report = IssueReportScheme(
            id = id,
            subject = subject.text.trim(),
            summary = summary.text.trim(),
            reporterId = currentUser.uid,
            reporterEmail = currentUser.email ?: "",
            appVersionName = BuildConfig.VERSION_NAME,
            appVersionCode = BuildConfig.VERSION_CODE.toLong(),
            androidVersion = Build.VERSION.RELEASE ?: "",
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}".trim(),
            diagnostics = if (diagnosticsRelevant && includeDiagnostics) diagnosticsAvailable else emptyList(),
            createdAt = System.currentTimeMillis()
        )

        db.collection("issueReports")
            .document(id)
            .set(report)
            .addOnSuccessListener {
                isSubmitting = false
                showSuccess = true
            }
            .addOnFailureListener { e ->
                isSubmitting = false
                Log.e("Firestore", "Failed to submit issue report: ${e.message}")
            }
    }

    LoadingScreenWrapper(
        isSubmitting,
        "Submitting report..."
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(20.dp)
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
                        "Report Issue",
                        style = MaterialTheme.typography.labelLarge,
                        letterSpacing = 0.5.sp,
                    )
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(15),
                            label = { Text("Subject") },
                            placeholder = { Text("Short summary of what went wrong") },
                            value = subject,
                            onValueChange = {
                                if (it.text.length <= MAX_SUBJECT_CHARS) subject = it
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            supportingText = {
                                Text(
                                    "${subject.text.length} / $MAX_SUBJECT_CHARS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }

                    item {
                        OutlinedTextField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            shape = RoundedCornerShape(15),
                            label = { Text("What happened?") },
                            placeholder = {
                                Text("The more detail, the easier to fix — e.g. which bank, what you expected")
                            },
                            value = summary,
                            onValueChange = {
                                if (it.text.length <= MAX_SUMMARY_CHARS) summary = it
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Default
                            ),
                            supportingText = {
                                Text(
                                    "${summary.text.length} / $MAX_SUMMARY_CHARS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }

                    if (diagnosticsRelevant && diagnosticsAvailable.isNotEmpty()) {
                        item {
                            DiagnosticsCard(
                                included = includeDiagnostics,
                                onIncludedChange = { includeDiagnostics = it },
                                entries = diagnosticsAvailable
                            )
                        }
                    }

                    item {
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { submit() },
                            enabled = subject.text.isNotBlank() && summary.text.isNotBlank()
                        ) {
                            Text("Submit", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }

        SuccessOverlay(
            visible = showSuccess,
            message = "Report submitted!",
            onDismiss = {
                showSuccess = false
                onBack()
            }
        )
    }
}

@Composable
private fun DiagnosticsCard(
    included: Boolean,
    onIncludedChange: (Boolean) -> Unit,
    entries: List<SmsDiagnosticEntry>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(15.dp)
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.BugReport,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    "  Attach recent SMS parsing info",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Switch(checked = included, onCheckedChange = onIncludedChange)
        }

        Text(
            "This looks related to SMS expense detection. Never includes the SMS text " +
                "itself, or full account/card numbers — only which bank sender, and whether " +
                "a merchant/account was found, for your last ${entries.size} messages.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (included) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                entries.take(10).forEach { entry ->
                    Text(
                        "${entry.sender} — ${entry.classification} — ${entry.outcome}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
