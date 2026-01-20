package com.settle.tracker.components.expenses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.settle.tracker.scheme.SplitParticipant
import com.settle.tracker.scheme.UserScheme

@Composable
fun EqualSplitMemberList(
    modifier: Modifier = Modifier,
    groupMembers: List<UserScheme>,
    selectedUserIds: Set<String>,
    splits: List<SplitParticipant>,
    onMemberSelectionChange: (String) -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        groupMembers.forEach { member ->
            val isSelected = member.id in selectedUserIds
            val splitAmount = splits.find { it.id == member.id }?.amount ?: 0.0

            EqualSplitMemberRow(
                member = member,
                isSelected = isSelected,
                splitAmount = splitAmount,
                onSelectionChange = {
                    onMemberSelectionChange(member.id)
                },
                minSelectionCount = if (selectedUserIds.size == 1 && isSelected) 1 else -1
            )
        }
    }
}
