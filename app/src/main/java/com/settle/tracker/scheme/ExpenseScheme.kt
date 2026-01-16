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
    MISC;

    fun getDisplayName() = this.name.lowercase().replaceFirstChar { it.uppercase() }
}

@IgnoreExtraProperties
data class ExpenseScheme(
    val id: String = "",
    val timestamp: Long = 0L,
    val details: String = "",
    val amount: Double = 0.0,
    val category: String = ExpenseCategory.MISC.name,
    val paidFrom: String = "",
    val createdAt: Long = 0L
)
