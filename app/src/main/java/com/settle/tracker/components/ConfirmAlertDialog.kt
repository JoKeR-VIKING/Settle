package com.settle.tracker.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ConfirmAlertDialog(
    title: String,
    text: String,
    subText: String,
    onConfirm: () -> Unit,
    toggleAlert: () -> Unit
) {
    AlertDialog(
        onDismissRequest = toggleAlert,
        title = {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text,
                    style = MaterialTheme.typography.labelMedium,
                    letterSpacing = 0.5.sp
                )

                Text(
                    subText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                )
            ) {
                Text(
                    "Delete",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = toggleAlert,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ),
                border = BorderStroke(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    "Cancel",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    )
}
