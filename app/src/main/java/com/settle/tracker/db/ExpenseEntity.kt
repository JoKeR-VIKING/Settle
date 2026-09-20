package com.settle.tracker.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expense_drafts")
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val amount: Double,
    val details: String,
    val category: String,
    val paidFrom: String,
    val timestamp: Long,
    /** The pattern that pre-filled this draft's details/category, if any — see SpendPatternEngine. */
    val patternId: String? = null
)
