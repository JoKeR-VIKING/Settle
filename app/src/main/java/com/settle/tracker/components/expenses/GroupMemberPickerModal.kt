package com.settle.tracker.components.expenses

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.settle.tracker.components.groups.GroupMemberList
import com.settle.tracker.scheme.GroupScheme
import com.settle.tracker.scheme.UserScheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupMemberPickerModal(
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    groupData: GroupScheme,
    onMemberSelected: (List<UserScheme>) -> Unit
) {
    ModalBottomSheet(
        modifier = Modifier.padding(16.dp),
        sheetState = sheetState,
        onDismissRequest = onDismissRequest
    ) {
        GroupMemberList(
            groupData = groupData,
            updateLoadingStatus = {},
            onClick = { user ->
                onMemberSelected(
                    listOf(
                        user
                    )
                )
                onDismissRequest()
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
