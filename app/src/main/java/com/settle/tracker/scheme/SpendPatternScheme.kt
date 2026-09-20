package com.settle.tracker.scheme

import com.google.firebase.firestore.IgnoreExtraProperties

enum class SpendPatternState {
    REVIEW,
    AUTO
}

@IgnoreExtraProperties
data class NegativeExampleScheme(
    val amount: Double = 0.0,
    val timeMinutes: Int = 0,
    val excludedAt: Long = 0L
)

/**
 * A recurring MISC-category personal spend, learned purely from amount +
 * time-of-day + the label/category the user assigns when correcting an SMS
 * draft — never from payee identity, since a driver's personal UPI handle
 * (the case this exists for) changes every ride. See [com.settle.tracker.sms.SpendPatternEngine].
 */
@IgnoreExtraProperties
data class SpendPatternScheme(
    val id: String = "",
    val label: String = "",
    val category: String = ExpenseCategory.MISC.name,
    val state: String = SpendPatternState.REVIEW.name,
    val amountMin: Double = 0.0,
    val amountMax: Double = 0.0,
    val timeStartMinutes: Int = 0,
    val timeEndMinutes: Int = 0,
    val negativeExamples: List<NegativeExampleScheme> = emptyList(),
    val confirmStreak: Int = 0,
    val occurrenceCount: Int = 0,
    val createdAt: Long = 0L,
    val lastMatchedAt: Long = 0L
)
