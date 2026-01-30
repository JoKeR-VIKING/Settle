package com.settle.tracker.scheme

import com.google.firebase.firestore.IgnoreExtraProperties

enum class RecurrenceType {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY;

    fun getDisplayName() = this.name.lowercase().replaceFirstChar { it.uppercase() }
}

@IgnoreExtraProperties
data class RecurringExpensesScheme(
    val id: String = "",

    val expenseData: ExpenseScheme = ExpenseScheme(),

    val frequency: String = RecurrenceType.MONTHLY.name,
    val interval: Int = 1,

    val startAt: Long = 0L,
    val nextOccurrenceAt: Long = 0L,
    val paused: Boolean = false,
    val durationInMonths: Int? = null,
    val endAt: Long? = null,

    val createdAt: Long = 0L
)
