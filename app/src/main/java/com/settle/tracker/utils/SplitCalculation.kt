package com.settle.tracker.utils

import com.settle.tracker.scheme.SplitParticipant
import com.settle.tracker.scheme.UserScheme

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
