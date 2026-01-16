package com.settle.tracker.scheme

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

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
    val id: String,
    val timestamp: Long,
    val details: String,
    val amount: Double,
    val category: String,
    val paidFrom: String,
    val createdAt: Long
)

@Parcelize
data class ExpenseDraft(
    val id: String? = "",
    val amount: Double = 0.0,
    val details: String = "",
    val category: String = ExpenseCategory.MISC.name,
    val paidFrom: String = "",
    val timestamp: Long,
    val smsExpenseId: String? = null
) : Parcelable
