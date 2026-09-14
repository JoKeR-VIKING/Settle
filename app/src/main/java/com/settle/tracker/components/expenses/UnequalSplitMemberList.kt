package com.settle.tracker.components.expenses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.settle.tracker.scheme.SplitParticipant
import com.settle.tracker.scheme.UserScheme
import com.settle.tracker.utils.formatCurrency
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.round

private fun formatShare(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else "%.2f".format(value)

@Composable
fun UnequalSplitMemberList(
    modifier: Modifier = Modifier,
    groupMembers: List<UserScheme>,
    splits: List<SplitParticipant>,
    totalAmount: Double = 0.0,
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

    // Members whose amount the user has manually typed. These stay fixed when
    // rebalancing; everyone else absorbs the remainder. Kept across the split
    // round-trips (only reset when the member list itself changes).
    var lockedIds by remember(groupMembers) { mutableStateOf(emptySet<String>()) }

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

    val enteredTotal = amountsMap.values.sumOf { it.toDoubleOrNull() ?: 0.0 }
    val remaining = totalAmount - enteredTotal
    val unlockedIds = groupMembers.map { it.id }.filter { it !in lockedIds }

    fun rebalance() {
        if (totalAmount <= 0.0 || unlockedIds.isEmpty()) return

        val lockedSum = groupMembers
            .filter { it.id in lockedIds }
            .sumOf { amountsMap[it.id]?.toDoubleOrNull() ?: 0.0 }

        val toDistribute = (totalAmount - lockedSum).coerceAtLeast(0.0)
        val perHead = round((toDistribute / unlockedIds.size) * 100) / 100
        var running = toDistribute

        val updates = unlockedIds.mapIndexed { index, id ->
            // Last unlocked member absorbs the rounding leftover so totals match.
            val share = if (index == unlockedIds.lastIndex) {
                round(running * 100) / 100
            } else {
                running -= perHead
                perHead
            }
            id to formatShare(share)
        }

        amountsMap = amountsMap + updates
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (totalAmount > 0.0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when {
                        remaining > 0.01 -> "${formatCurrency(remaining)} left to assign"
                        remaining < -0.01 -> "${formatCurrency(-remaining)} over the total"
                        else -> "All assigned"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = when {
                        remaining < -0.01 -> MaterialTheme.colorScheme.error
                        remaining > 0.01 -> MaterialTheme.colorScheme.onSurfaceVariant
                        else -> MaterialTheme.colorScheme.primary
                    },
                    textAlign = TextAlign.Start
                )

                if (abs(remaining) > 0.01 && unlockedIds.isNotEmpty()) {
                    TextButton(onClick = { rebalance() }) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "  Split rest",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }

        groupMembers.forEach { member ->
            val currentAmount = amountsMap[member.id] ?: ""

            UnequalSplitMemberRow(
                member = member,
                amount = currentAmount,
                onAmountChange = { newAmount ->
                    // Typing locks the member; clearing the field returns them to
                    // the pool that "Split rest" redistributes into.
                    lockedIds = if (newAmount.isBlank()) {
                        lockedIds - member.id
                    } else {
                        lockedIds + member.id
                    }
                    amountsMap = amountsMap + (member.id to newAmount)
                }
            )
        }
    }
}
