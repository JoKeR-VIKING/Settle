package com.settle.tracker.components.account

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

@Composable
fun UpiIdField(
    modifier: Modifier = Modifier,
    upiId: TextFieldValue,
    upiIdSynced: String,
    isEditing: Boolean,
    onUpiIdChange: (TextFieldValue) -> Unit,
    onSave: () -> Unit,
    focusRequester: FocusRequester
) {
    OutlinedTextField(
        modifier = modifier
            .fillMaxWidth(0.95f)
            .focusRequester(focusRequester),
        textStyle = MaterialTheme.typography.labelLarge,
        shape = RoundedCornerShape(15),
        label = { Text("Your UPI ID", style = MaterialTheme.typography.labelMedium) },
        placeholder = { Text("example@ok_icici") },
        value = upiId,
        onValueChange = onUpiIdChange,
        enabled = !isEditing,
        singleLine = true,
        trailingIcon = {
            if (upiId.text != upiIdSynced) {
                IconButton(onClick = onSave) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Save UPI ID Changes",
                    )
                }
            }
        }
    )
}
