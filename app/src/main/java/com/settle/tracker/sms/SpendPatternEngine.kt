package com.settle.tracker.sms

import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import kotlin.math.abs

/**
 * Learns a user's recurring MISC-category spends (e.g. a daily auto-rickshaw
 * ride paid to a driver's personal, ever-changing UPI handle) purely from
 * amount + time-of-day + the label/category the user assigns when correcting
 * an SMS draft — never from the raw unedited SMS guess, and never from
 * payee identity, since that's the one thing these transactions don't keep
 * stable.
 *
 * The signal chain: a user reviewing an SMS_ADD draft that parsed to MISC
 * edits it to something real ("Auto to office" / TRAVEL) — that correction,
 * not the original guess, is the seed. Once enough corrections agree
 * ([tryFormCluster]), a pattern is born in REVIEW state: future matching
 * drafts are pre-filled from it, but still require a tap-to-save. Every
 * unedited save nudges it toward AUTO ([recordConfirmation]); every edited
 * save teaches it a point to never match again ([matchPattern]'s negative
 * exclusion), without touching the pattern's positive bounds — one bad
 * match shouldn't blind it to everything else nearby.
 */
object SpendPatternEngine {
    /** How many distinct-day corrections must agree before a pattern is born. */
    const val DISCOVERY_MIN_OCCURRENCES = 5

    /** Consecutive unedited confirmations before a pattern goes fully silent. */
    const val GRADUATION_STREAK_THRESHOLD = 5

    /** A transaction this close (minutes) to a negative example is excluded, even inside the positive box. */
    const val NEGATIVE_TIME_EPSILON_MINUTES = 10

    /** A transaction this close (rupees) to a negative example is excluded, even inside the positive box. */
    const val NEGATIVE_AMOUNT_EPSILON = 5.0

    fun normalizeLabel(label: String): String =
        label.trim().lowercase(Locale.getDefault()).replace(Regex("\\s+"), " ")

    fun minutesOfDay(timestampMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Int {
        val time = Instant.ofEpochMilli(timestampMillis).atZone(zone).toLocalTime()
        return time.hour * 60 + time.minute
    }

    private fun dateKey(timestampMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(timestampMillis).atZone(zone).toLocalDate().toString()

    /** A single corrected SMS draft, sitting in the candidate pool awaiting enough company to form a pattern. */
    data class Candidate(
        val label: String,
        val category: String,
        val amount: Double,
        val timestampMillis: Long
    )

    data class DiscoveredPattern(
        val label: String,
        val category: String,
        val amountMin: Double,
        val amountMax: Double,
        val timeStartMinutes: Int,
        val timeEndMinutes: Int,
        val memberCount: Int
    )

    /**
     * Checks whether [newCandidate], combined with everything already sitting
     * in [existingCandidates] for the same normalized label + category, is
     * now enough evidence to form a pattern. Requires both a minimum count
     * AND a minimum number of distinct calendar days — otherwise a single
     * busy day with several similarly-priced, similarly-timed but unrelated
     * corrections could masquerade as a recurring habit. Returns null if not
     * yet there; the caller adds the candidate to the pool regardless.
     */
    fun tryFormCluster(
        newCandidate: Candidate,
        existingCandidates: List<Candidate>
    ): DiscoveredPattern? {
        val label = normalizeLabel(newCandidate.label)
        val members = (existingCandidates + newCandidate)
            .filter { normalizeLabel(it.label) == label && it.category == newCandidate.category }

        val distinctDays = members.map { dateKey(it.timestampMillis) }.toSet()
        if (members.size < DISCOVERY_MIN_OCCURRENCES || distinctDays.size < DISCOVERY_MIN_OCCURRENCES) {
            return null
        }

        val minutes = members.map { minutesOfDay(it.timestampMillis) }
        return DiscoveredPattern(
            label = label,
            category = newCandidate.category,
            amountMin = members.minOf { it.amount },
            amountMax = members.maxOf { it.amount },
            timeStartMinutes = minutes.min(),
            timeEndMinutes = minutes.max(),
            memberCount = members.size
        )
    }

    /** The bits of a stored pattern [matchPattern] needs — decoupled from Room/Firestore types. */
    data class MatchablePattern(
        val id: String,
        val state: String, // SpendPatternState.name
        val amountMin: Double,
        val amountMax: Double,
        val timeStartMinutes: Int,
        val timeEndMinutes: Int,
        val negatives: List<Pair<Double, Int>> // (amount, timeMinutes)
    )

    sealed class MatchOutcome {
        data object NoMatch : MatchOutcome()
        data class Review(val patternId: String) : MatchOutcome()
        data class Auto(val patternId: String) : MatchOutcome()
    }

    /**
     * Matches a freshly-parsed MISC transaction against the user's known
     * patterns. Only same-day time windows are supported — a pattern
     * spanning midnight (11pm-1am) won't match correctly in this version.
     */
    fun matchPattern(
        amount: Double,
        timestampMillis: Long,
        patterns: List<MatchablePattern>
    ): MatchOutcome {
        val minutes = minutesOfDay(timestampMillis)

        val matching = patterns.filter { p ->
            amount in p.amountMin..p.amountMax &&
                minutes in p.timeStartMinutes..p.timeEndMinutes &&
                p.negatives.none { (negAmount, negMinutes) ->
                    abs(negAmount - amount) <= NEGATIVE_AMOUNT_EPSILON &&
                        abs(negMinutes - minutes) <= NEGATIVE_TIME_EPSILON_MINUTES
                }
        }

        val best = matching.minByOrNull { p ->
            val amountCenter = (p.amountMin + p.amountMax) / 2
            val timeCenter = (p.timeStartMinutes + p.timeEndMinutes) / 2
            abs(amountCenter - amount) + abs(timeCenter - minutes)
        } ?: return MatchOutcome.NoMatch

        return if (best.state == "AUTO") MatchOutcome.Auto(best.id) else MatchOutcome.Review(best.id)
    }

    /** What happens to a REVIEW-state pattern after the user saves (or doesn't edit) its suggestion. */
    data class ConfirmationResult(
        val newConfirmStreak: Int,
        val newState: String,
        val negativeExample: Pair<Double, Int>?
    )

    fun recordConfirmation(
        currentStreak: Int,
        wasEdited: Boolean,
        amount: Double,
        timestampMillis: Long
    ): ConfirmationResult {
        if (wasEdited) {
            return ConfirmationResult(
                newConfirmStreak = 0,
                newState = "REVIEW",
                negativeExample = amount to minutesOfDay(timestampMillis)
            )
        }

        val streak = currentStreak + 1
        val state = if (streak >= GRADUATION_STREAK_THRESHOLD) "AUTO" else "REVIEW"
        return ConfirmationResult(newConfirmStreak = streak, newState = state, negativeExample = null)
    }
}
