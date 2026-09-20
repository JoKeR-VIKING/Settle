package com.settle.tracker.scheme

import com.google.firebase.firestore.IgnoreExtraProperties

enum class ExpenseCategory {
    FOOD,
    GROCERY,
    DRINKS,
    ENTERTAINMENT,
    SHOPPING,
    SUBSCRIPTION,
    HEALTH,
    TRAVEL,
    INVESTMENT,
    GIFT,
    MISC,
    SETTLEMENT;

    fun getDisplayName() = this.name.lowercase().replaceFirstChar { it.uppercase() }
}

enum class SplitMode {
    EQUAL,
    UNEQUAL
}

@IgnoreExtraProperties
data class SplitParticipant(
    val id: String = "",
    val name: String = "",
    val amount: Double = 0.0
)

@IgnoreExtraProperties
data class ExpenseScheme(
    val id: String = "",
    val timestamp: Long = 0L,
    val details: String = "",
    val amount: Double = 0.0,
    val category: String = ExpenseCategory.MISC.name,
    val paidFrom: String = "",
    val paidBy: List<SplitParticipant> = emptyList(),
    val splitMode: String = SplitMode.EQUAL.name,
    val splits: List<SplitParticipant> = emptyList(),
    val createdAt: Long = 0L,
    val recurringTemplateId: String? = null,
    val source: String = ExpenseSource.MANUAL.name,
    val patternId: String? = null
)

enum class ExpenseSource {
    MANUAL,
    SMS_REVIEWED,
    SMS_AUTO
}
