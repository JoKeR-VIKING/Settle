package com.settle.tracker.components.expenses

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.settle.tracker.components.groups.GroupMemberList
import com.settle.tracker.scheme.GroupScheme
import com.settle.tracker.scheme.SplitParticipant
import com.settle.tracker.scheme.UserScheme
import com.settle.tracker.utils.fetchGroupMembersChunked

enum class PayerType {
    SINGLE,
    MULTI
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupMemberPickerModal(
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    groupData: GroupScheme,
    paidBy: List<SplitParticipant>,
    onMemberSelected: (List<SplitParticipant>) -> Unit,
    amount: Double
) {
    val db = Firebase.firestore

    var groupMembers by remember { mutableStateOf(emptyList<UserScheme>()) }
    var payers by remember { mutableStateOf(paidBy) }
    var selectedTab by remember { mutableStateOf(PayerType.SINGLE) }

    LaunchedEffect(groupData.members) {
        fetchGroupMembersChunked(
            memberIds = groupData.members,
            updateLoadingStatus = {},
            db = db,
            updateGroupMembers = {
                groupMembers = it
            }
        )
    }

    ModalBottomSheet(
        modifier = Modifier.padding(16.dp),
        sheetState = sheetState,
        onDismissRequest = onDismissRequest
    ) {
        TabRow(
            selectedTabIndex = selectedTab.ordinal,
        ) {
            Tab(
                selected = selectedTab == PayerType.SINGLE,
                onClick = { selectedTab = PayerType.SINGLE },
                text = {
                    Text(
                        "Single Payer",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )

            Tab(
                selected = selectedTab == PayerType.MULTI,
                onClick = { selectedTab = PayerType.MULTI },
                text = {
                    Text(
                        "Multi Payer",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
        }

        if (selectedTab == PayerType.SINGLE) {
            GroupMemberList(
                groupData = groupData,
                groupMembers = groupMembers,
                updateLoadingStatus = {},
                onClick = { user ->
                    onMemberSelected(
                        listOf(
                            SplitParticipant(
                                id = user.id,
                                name = user.name,
                                amount = amount
                            )
                        )
                    )
                    onDismissRequest()
                },
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            UnequalSplitMemberList(
                modifier = Modifier.fillMaxWidth(),
                groupMembers = groupMembers,
                splits = payers,
                onSplitsChange = {
                    payers = it
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = amount > 0.0 && payers.sumOf { it.amount } == amount,
                onClick = {
                    onMemberSelected(payers)
                    onDismissRequest()
                }
            ) {
                Text(
                    "Done",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
