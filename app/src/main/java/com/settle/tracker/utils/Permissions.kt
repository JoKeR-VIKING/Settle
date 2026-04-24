package com.settle.tracker.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

/**
 * Lightweight on-demand permission requester for Compose.
 *
 * Usage:
 *   val sms = rememberPermissionRequester(
 *       permission = Manifest.permission.READ_SMS,
 *       rationaleTitle = "Read your SMS",
 *       rationaleBody = "…"
 *   ) { granted -> if (granted) openSmsImport() }
 *
 *   Button(onClick = sms::request) { … }
 *
 * Renders an inline rationale dialog before the system prompt the first time.
 */
class PermissionRequester internal constructor(
    private val onRequest: () -> Unit,
) {
    fun request() = onRequest()
}

enum class SettlePermission(
    val androidKey: String,
    val label: String,
    val why: String,
    val icon: ImageVector,
    val minApi: Int = Build.VERSION_CODES.BASE
) {
    ReadSms(
        androidKey = Manifest.permission.READ_SMS,
        label = "Read SMS",
        why = "We scan bank SMS on-device to pre-fill expenses. Nothing is uploaded.",
        icon = Icons.Filled.Sms
    ),
    ReceiveSms(
        androidKey = Manifest.permission.RECEIVE_SMS,
        label = "Receive SMS",
        why = "So new transaction SMS can be instantly picked up as a draft.",
        icon = Icons.Filled.Sms
    ),
    Notifications(
        androidKey = "android.permission.POST_NOTIFICATIONS",
        label = "Notifications",
        why = "Reminders for recurring expenses & new transaction alerts.",
        icon = Icons.Filled.Notifications,
        minApi = Build.VERSION_CODES.TIRAMISU
    ),
    Contacts(
        androidKey = Manifest.permission.READ_CONTACTS,
        label = "Contacts",
        why = "Quickly add friends to groups from your contacts. Stays on device.",
        icon = Icons.Filled.Contacts
    );
}

fun Context.hasPermission(p: SettlePermission): Boolean {
    if (Build.VERSION.SDK_INT < p.minApi) return true
    return ContextCompat.checkSelfPermission(this, p.androidKey) == PackageManager.PERMISSION_GRANTED
}

@Composable
fun rememberPermissionRequester(
    permission: SettlePermission,
    onResult: (Boolean) -> Unit
): PermissionRequester {
    val context = LocalContext.current
    var showRationale by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        onResult(granted)
    }

    val trigger = {
        if (Build.VERSION.SDK_INT < permission.minApi) {
            onResult(true)
        } else if (context.hasPermission(permission)) {
            onResult(true)
        } else {
            showRationale = true
        }
    }

    if (showRationale) {
        AlertDialog(
            onDismissRequest = { showRationale = false },
            shape = RoundedCornerShape(22.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            icon = {
                Icon(
                    imageVector = permission.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    "Enable ${permission.label}?",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column {
                    Text(
                        permission.why,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "You can revoke this anytime from Settings.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showRationale = false
                    launcher.launch(permission.androidKey)
                }) { Text("Allow", style = MaterialTheme.typography.labelLarge) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRationale = false
                    onResult(false)
                }) { Text("Not now") }
            }
        )
    }

    return remember { PermissionRequester(trigger) }
}
