package com.settle.tracker

import com.settle.tracker.sms.SpendPatternEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

private fun at(daysAgo: Int, hour: Int, minute: Int): Long =
    LocalDate.now().minusDays(daysAgo.toLong())
        .atTime(LocalTime.of(hour, minute))
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

class SpendPatternEngineTest {

    // ---- Discovery ----

    @Test
    fun fewerThanFiveCorrectionsDoNotFormAPattern() {
        val corrections = (1..4).map { day ->
            SpendPatternEngine.Candidate("Auto to office", "TRAVEL", 120.0, at(day, 8, 30))
        }
        val result = SpendPatternEngine.tryFormCluster(corrections.last(), corrections.dropLast(1))
        assertNull(result)
    }

    @Test
    fun fiveCorrectionsOnTheSameDayDoNotFormAPattern() {
        // The 50-transactions-in-one-day case: same label, same day, doesn't count.
        val corrections = (1..5).map {
            SpendPatternEngine.Candidate("Auto to office", "TRAVEL", 120.0 + it, at(0, 8 + it, 0))
        }
        val result = SpendPatternEngine.tryFormCluster(corrections.last(), corrections.dropLast(1))
        assertNull(result)
    }

    @Test
    fun fiveDistinctDayCorrectionsFormAPatternWithBoundsFromObservedData() {
        val corrections = listOf(
            SpendPatternEngine.Candidate("Auto to office", "TRAVEL", 110.0, at(5, 8, 15)),
            SpendPatternEngine.Candidate("Auto to office", "TRAVEL", 125.0, at(4, 8, 40)),
            SpendPatternEngine.Candidate("Auto to office", "TRAVEL", 130.0, at(3, 9, 5)),
            SpendPatternEngine.Candidate("Auto to office", "TRAVEL", 118.0, at(2, 8, 55)),
            SpendPatternEngine.Candidate("auto to office", "TRAVEL", 140.0, at(1, 9, 20)) // case-insensitive match
        )

        val result = SpendPatternEngine.tryFormCluster(corrections.last(), corrections.dropLast(1))

        assertTrue(result != null)
        result!!
        assertEquals("auto to office", result.label)
        assertEquals("TRAVEL", result.category)
        assertEquals(110.0, result.amountMin, 0.001)
        assertEquals(140.0, result.amountMax, 0.001)
        assertEquals(8 * 60 + 15, result.timeStartMinutes)
        assertEquals(9 * 60 + 20, result.timeEndMinutes)
        assertEquals(5, result.memberCount)
    }

    @Test
    fun differentLabelsDoNotCluster() {
        val corrections = listOf(
            SpendPatternEngine.Candidate("Auto to office", "TRAVEL", 120.0, at(5, 8, 30)),
            SpendPatternEngine.Candidate("Coffee", "DRINKS", 120.0, at(4, 8, 30)),
            SpendPatternEngine.Candidate("Coffee", "DRINKS", 120.0, at(3, 8, 30)),
            SpendPatternEngine.Candidate("Coffee", "DRINKS", 120.0, at(2, 8, 30))
        )
        val newOne = SpendPatternEngine.Candidate("Auto to office", "TRAVEL", 121.0, at(1, 8, 30))
        // Only 2 "Auto to office" entries total (the seed above + this one) — not enough.
        assertNull(SpendPatternEngine.tryFormCluster(newOne, corrections))
    }

    // ---- Matching ----

    private fun reviewPattern(negatives: List<Pair<Double, Int>> = emptyList()) = SpendPatternEngine.MatchablePattern(
        id = "p1",
        state = "REVIEW",
        amountMin = 110.0,
        amountMax = 140.0,
        timeStartMinutes = 8 * 60 + 15,
        timeEndMinutes = 9 * 60 + 20,
        negatives = negatives
    )

    @Test
    fun transactionInsideBoundsMatchesReviewPattern() {
        val outcome = SpendPatternEngine.matchPattern(
            amount = 122.0,
            timestampMillis = at(0, 8, 45),
            patterns = listOf(reviewPattern())
        )
        assertEquals(SpendPatternEngine.MatchOutcome.Review("p1"), outcome)
    }

    @Test
    fun transactionOutsideAmountBoundsDoesNotMatch() {
        val outcome = SpendPatternEngine.matchPattern(
            amount = 800.0,
            timestampMillis = at(0, 8, 45),
            patterns = listOf(reviewPattern())
        )
        assertEquals(SpendPatternEngine.MatchOutcome.NoMatch, outcome)
    }

    @Test
    fun autoStatePatternReturnsAutoOutcome() {
        val autoPattern = reviewPattern().copy(state = "AUTO")
        val outcome = SpendPatternEngine.matchPattern(
            amount = 122.0,
            timestampMillis = at(0, 8, 45),
            patterns = listOf(autoPattern)
        )
        assertEquals(SpendPatternEngine.MatchOutcome.Auto("p1"), outcome)
    }

    @Test
    fun negativeExampleExcludesNearbyTransactionWithoutShrinkingTheWholeBox() {
        // A genuine ₹120 breakfast at 9:00am was corrected away from this pattern once.
        val patternWithNegative = reviewPattern(negatives = listOf(120.0 to (9 * 60)))

        val nearNegative = SpendPatternEngine.matchPattern(
            amount = 121.0, // within epsilon of the negative
            timestampMillis = at(0, 9, 2),
            patterns = listOf(patternWithNegative)
        )
        assertEquals(SpendPatternEngine.MatchOutcome.NoMatch, nearNegative)

        // But a different point still inside the box, far from the negative, still matches.
        val elsewhereInBox = SpendPatternEngine.matchPattern(
            amount = 115.0,
            timestampMillis = at(0, 8, 20),
            patterns = listOf(patternWithNegative)
        )
        assertEquals(SpendPatternEngine.MatchOutcome.Review("p1"), elsewhereInBox)
    }

    // ---- Graduation ----

    @Test
    fun unchangedConfirmationsAccumulateAndGraduate() {
        var streak = 0
        var state = "REVIEW"

        repeat(SpendPatternEngine.GRADUATION_STREAK_THRESHOLD) {
            val result = SpendPatternEngine.recordConfirmation(
                currentStreak = streak,
                wasEdited = false,
                amount = 120.0,
                timestampMillis = at(0, 8, 45)
            )
            streak = result.newConfirmStreak
            state = result.newState
        }

        assertEquals(SpendPatternEngine.GRADUATION_STREAK_THRESHOLD, streak)
        assertEquals("AUTO", state)
    }

    @Test
    fun anEditedSaveResetsStreakAndRecordsANegativeAtTheOriginalPoint() {
        val result = SpendPatternEngine.recordConfirmation(
            currentStreak = 3,
            wasEdited = true,
            amount = 120.0,
            timestampMillis = at(0, 9, 0)
        )

        assertEquals(0, result.newConfirmStreak)
        assertEquals("REVIEW", result.newState)
        assertEquals(120.0 to (9 * 60), result.negativeExample)
    }
}
