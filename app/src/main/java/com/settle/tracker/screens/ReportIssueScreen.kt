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
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.settle.tracker.utils.SettlePrefs
import com.settle.tracker.utils.captureOwnProcessLogs
import com.settle.tracker.utils.mentionsSmsIssue
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val MAX_SUBJECT_CHARS = 80
private const val MAX_SUMMARY_CHARS = 600

@Composable
fun ReportIssueScreen(
    onBack: () -> Unit,
    currentUser: FirebaseUser
) {
    val db = Firebase.firestore
    val context = LocalContext.current
    val prefs = remember { SettlePrefs(context.applicationContext) }

    var subject by remember { mutableStateOf(TextFieldValue("")) }
    var summary by remember { mutableStateOf(TextFieldValue("")) }
    var includeDiagnostics by remember { mutableStateOf(true) }
    var includeDeviceLogs by remember { mutableStateOf(true) }
    var includeCrashInfo by remember { mutableStateOf(true) }
    var isSubmitting by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }
    var deviceLogs by remember { mutableStateOf<String?>(null) }
    var lastCrash by remember { mutableStateOf<String?>(null) }

    val diagnosticsRelevant = mentionsSmsIssue(subject.text, summary.text)
    val diagnosticsAvailable = remember(diagnosticsRelevant) {
        if (diagnosticsRelevant) SmsDiagnostics.recent() else emptyList()
    }

    LaunchedEffect(Unit) {
        lastCrash = prefs.readLastCrash()
        deviceLogs = withContext(Dispatchers.IO) { captureOwnProcessLogs() }
    }

    fun submit() {
        isSubmitting = true

        val id = UUID.randomUUID().toString()
        val crashAttached = lastCrash != null && includeCrashInfo
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
            deviceLogs = if (includeDeviceLogs) deviceLogs else null,
            crashInfo = if (crashAttached) lastCrash else null,
            createdAt = System.currentTimeMillis()
        )

        db.collection("issueReports")
            .document(id)
            .set(report)
            .addOnSuccessListener {
                isSubmitting = false
                if (crashAttached) prefs.clearLastCrash()
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

                    lastCrash?.let { crash ->
                        item {
                            CrashCard(
                                included = includeCrashInfo,
                                onIncludedChange = { includeCrashInfo = it },
                                crash = crash
                            )
                        }
                    }

                    deviceLogs?.let { logs ->
                        item {
                            DeviceLogsCard(
                                included = includeDeviceLogs,
                                onIncludedChange = { includeDeviceLogs = it },
                                logs = logs
                            )
                        }
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

@Composable
private fun CrashCard(
    included: Boolean,
    onIncludedChange: (Boolean) -> Unit,
    crash: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.errorContainer,
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
                    Icons.Filled.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    "  We noticed a crash last session",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Switch(checked = included, onCheckedChange = onIncludedChange)
        }

        Text(
            "Attach its stack trace to help track this down. It's cleared once you " +
                "attach it, so it won't show up again on future reports.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onErrorContainer
        )

        if (included) {
            Text(
                crash.lineSequence().take(6).joinToString("\n"),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                maxLines = 6
            )
        }
    }
}

@Composable
private fun DeviceLogsCard(
    included: Boolean,
    onIncludedChange: (Boolean) -> Unit,
    logs: String
) {
    val lineCount = logs.lineSequence().count()

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
                    Icons.Filled.Terminal,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    "  Attach recent app logs",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Switch(checked = included, onCheckedChange = onIncludedChange)
        }

        Text(
            "Useful for crashes or a blank/frozen screen. Only this app's own log " +
                "lines ($lineCount recent) — never other apps' or system-wide logs, and " +
                "anything that looks like a token or email is stripped first.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (included) {
            Text(
                logs.lineSequence().toList().takeLast(6).joinToString("\n"),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 6
            )
        }
    }
}
