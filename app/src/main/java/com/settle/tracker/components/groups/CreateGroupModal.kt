package com.settle.tracker.components.groups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.settle.tracker.screens.MAX_GROUP_NAME_CHARS

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupModal(
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    groupName: TextFieldValue,
    onGroupNameChange: (TextFieldValue) -> Unit,
    isSubmitting: Boolean,
    onCreateGroup: () -> Unit
) {
    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(0.90f),
                textStyle = MaterialTheme.typography.labelLarge,
                shape = RoundedCornerShape(15),
                label = {
                    Text(
                        "Group Name",
                        style = MaterialTheme.typography.labelLarge
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
                singleLine = true
            )

            Button(
                modifier = Modifier.fillMaxWidth(0.90f),
                onClick = onCreateGroup,
                enabled = !isSubmitting && groupName.text.isNotBlank(),
                shape = RoundedCornerShape(25),
            ) {
                Text(
                    "Add Group",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
