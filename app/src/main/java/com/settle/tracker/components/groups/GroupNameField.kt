package com.settle.tracker.components.groups

import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import com.settle.tracker.screens.MAX_GROUP_NAME_CHARS

@Composable
fun GroupNameField(
    modifier: Modifier = Modifier,
    groupName: TextFieldValue,
    originalGroupName: String,
    onGroupNameChange: (TextFieldValue) -> Unit,
    isEditing: Boolean,
    onSave: () -> Unit
) {
    OutlinedTextField(
        modifier = modifier.fillMaxWidth(0.85f),
        textStyle = MaterialTheme.typography.labelMedium,
        shape = RoundedCornerShape(15),
        label = {
            Text(
                "Group Name",
                style = MaterialTheme.typography.labelMedium
            )
        },
        supportingText = {
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "${groupName.text.length} / $MAX_GROUP_NAME_CHARS",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(Alignment.CenterEnd),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        value = groupName,
        onValueChange = {
            if (it.text.length <= MAX_GROUP_NAME_CHARS) {
                onGroupNameChange(it)
            }
        },
        enabled = !isEditing,
        singleLine = true,
        trailingIcon = {
            if (groupName.text != originalGroupName) {
                IconButton(onClick = onSave) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Save Group Name Changes",
                    )
                }
            }
        }
    )
}
