package com.settle.tracker.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "spend_patterns")
data class SpendPatternEntity(
    @PrimaryKey val id: String,
    val label: String,
    val category: String,
    val state: String,
    val amountMin: Double,
    val amountMax: Double,
    val timeStartMinutes: Int,
    val timeEndMinutes: Int,
    val confirmStreak: Int,
    val occurrenceCount: Int,
    val createdAt: Long,
    val lastMatchedAt: Long
)

@Entity(tableName = "spend_pattern_negatives")
data class SpendPatternNegativeEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val patternId: String,
    val amount: Double,
    val timeMinutes: Int,
    val excludedAt: Long
)

/**
 * A single SMS draft the user corrected away from the raw MISC guess,
 * sitting in a pool until enough similar corrections accumulate to form a
 * [SpendPatternEntity] (see SpendPatternEngine.tryFormCluster).
 */
@Entity(tableName = "pattern_candidates")
data class PatternCandidateEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val label: String,
    val category: String,
    val amount: Double,
    val transactionDate: String,
    val timestampMillis: Long,
    val createdAt: Long
)
