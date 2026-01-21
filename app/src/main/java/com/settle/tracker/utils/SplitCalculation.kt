package com.settle.tracker.utils

import com.settle.tracker.components.groups.EPSILON_VALUE
import com.settle.tracker.scheme.ExpenseScheme
import com.settle.tracker.scheme.SplitParticipant
import com.settle.tracker.scheme.UserScheme
import kotlin.math.abs

fun recalculateEqualSplit(
    groupMembers: List<UserScheme>,
    selectedUserIds: Set<String>,
    totalAmount: Double
): List<SplitParticipant> {
    val selectedMembers = groupMembers.filter { it.id in selectedUserIds }
    if (selectedMembers.isEmpty() || totalAmount <= 0) return emptyList()

    val totalPaise = (totalAmount * 100).toInt()
    val count = selectedMembers.size

    val baseShare = totalPaise / count
    val remainder = totalPaise % count

    return selectedMembers.mapIndexed { index, member ->
        val userShare = if (index < remainder) baseShare + 1 else baseShare

        SplitParticipant(
            id = member.id,
            name = member.name,
            amount = userShare / 100.0
        )
    }
}

fun calculateNetBalances(
    expenses: List<ExpenseScheme>
): Map<String, Double> {
    val balances = mutableMapOf<String, Double>()

    expenses.forEach { expense ->
        expense.paidBy.forEach {
            balances[it.id] = (balances[it.id] ?: 0.0) + it.amount
        }

        expense.splits.forEach {
            balances[it.id] = (balances[it.id] ?: 0.0) - it.amount
        }
    }

    return balances
        .mapValues { (_, amount) ->
            if (abs(amount) < EPSILON_VALUE) 0.0 else amount
        }
        .filterValues { it != 0.0 }
}

