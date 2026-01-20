package com.settle.tracker.components.expenses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.settle.tracker.scheme.SplitParticipant
import com.settle.tracker.scheme.UserScheme
import kotlinx.coroutines.delay

@Composable
fun UnequalSplitMemberList(
    modifier: Modifier = Modifier,
    groupMembers: List<UserScheme>,
    splits: List<SplitParticipant>,
    onSplitsChange: (List<SplitParticipant>) -> Unit
) {
    val splitsMap = remember(splits) {
        splits.associate { it.id to it.amount.toString() }
    }

    var amountsMap by remember(groupMembers, splitsMap) {
        mutableStateOf(
            groupMembers.associate { member ->
                member.id to (splitsMap[member.id] ?: "")
            }
        )
    }

    LaunchedEffect(amountsMap) {
        delay(400)

        val updatedSplits = groupMembers.map { member ->
            val amountStr = amountsMap[member.id] ?: ""
            val amount = amountStr.toDoubleOrNull() ?: 0.0

            SplitParticipant(
                id = member.id,
                name = member.name,
                amount = amount
            )
        }.filter { it.amount > 0 }

        onSplitsChange(updatedSplits)
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        groupMembers.forEach { member ->
            val currentAmount = amountsMap[member.id] ?: ""

            UnequalSplitMemberRow(
                member = member,
                amount = currentAmount,
                onAmountChange = { newAmount ->
                    amountsMap = amountsMap + (member.id to newAmount)
                }
            )
        }
    }
}
