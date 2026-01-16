package com.settle.tracker.sms

import com.settle.tracker.scheme.ExpenseCategory
import com.settle.tracker.scheme.ExpenseScheme
import java.security.MessageDigest
import java.util.Locale

object SmsParse {
    private val debitKeywords =
        listOf("spent", "debit", "debited", "txn", "sent", "paid", "payment")
    private val creditKeywords = listOf(
        "credit",
        "credited",
        "received",
        "deposited",
        "added",
        "refund",
        "reversal",
        "cashback"
    )
    private val categoryKeywords: Map<ExpenseCategory, List<String>> = mapOf(
        ExpenseCategory.FOOD to listOf(
            "zomato", "swiggy", "restaurant", "hotel", "brew", "food", "diner", "eatery"
        ),

        ExpenseCategory.GROCERY to listOf(
            "blinkit", "zepto", "instamart", "hypermarket",
            "supermarket", "grocery", "market", "reliance",
            "retail"
        ),

        ExpenseCategory.DRINKS to listOf(
            "coffee", "starbucks", "tea", "cafe"
        ),

        ExpenseCategory.ENTERTAINMENT to listOf(
            "bookmyshow", "district", "movie", "cinema",
            "theatre", "timezone"
        ),

        ExpenseCategory.SHOPPING to listOf(
            "amazon", "flipkart", "myntra",
            "zara", "h&m", "zudio", "westside",
            "pantaloons", "lifestyle", "max",
            "clothing", "clothes", "fashion"
        ),

        ExpenseCategory.SUBSCRIPTION to listOf(
            "netflix", "youtube", "prime", "amazon prime",
            "spotify", "hotstar", "crunchyroll"
        ),

        ExpenseCategory.HEALTH to listOf(
            "hospital", "pharmacy", "clinic",
            "medical", "health", "labs"
        ),

        ExpenseCategory.TRAVEL to listOf(
            "uber", "ola", "rapido", "nammayatri",
            "cab", "taxi", "bike", "auto", "car"
        )
    )
    private val amountRegex = Regex("""(?i)(rs\.?|inr)\s*[:.]?\s*([\d,]+(?:\.\d{1,2})?)""")
    private val detailsRegexOne = Regex("""(?i)\b(at|to)\s+([^\s.\n]+)""")
    private val detailsRegexTwo = Regex("""(?i)\b(on)\s+([^\s.\n]+)""")
    private val cardRegex = Regex("""(?i)(card|credit card).*?(?:xx|ending)?\s*(\d{4})""")
    private val amazonPayRegex = Regex("""(?i)\bapay\s+(wallet\s+)?balance\b""")
    private val accountRegex = Regex("""(?i)(a/c|account).*?(\*+\d{4}|\d{4})""")

    fun parse(
        sender: String,
        body: String,
        receivedAt: Long,
    ): ExpenseScheme? {
        val text = body.lowercase(Locale.getDefault())

        if (
            debitKeywords.none { text.contains(it) } &&
            creditKeywords.any { text.contains(it) }
        )
            return null

        val amount = extractAmount(text)
        if (amount == 0.0) return null

        val paidFrom = extractPaidFrom(text)
        if (paidFrom == "") return null

        val id = generateFingerprint(sender, body, receivedAt)
        val details = extractDetails(sender, text)
        val category = extractCategory(text)

        return ExpenseScheme(
            id = id,
            amount = amount,
            details = details,
            paidFrom = paidFrom,
            category = category.name,
            timestamp = receivedAt
        )
    }

    private fun extractAmount(text: String): Double {
        val match = amountRegex.find(text) ?: return 0.0
        return match
            .groupValues[2]
            .replace(",", "")
            .toDoubleOrNull() ?: 0.0
    }

    private fun isValidDetail(candidate: String): Boolean {
        if (candidate.length < 3) return false
        if (
            candidate.none { it.isLetter() } ||
            candidate.all { it.isDigit() }
        )
            return false

        if (candidate.matches(Regex("""(?i)\d{2,4}[-/](?:\d{2}|[A-Za-z]{3})[-/]\d{2,4}"""))) return false

        return true
    }

    private fun extractDetails(
        sender: String,
        text: String
    ): String {
        detailsRegexOne.findAll(text).forEach { match ->
            val candidate = match.groupValues[2].trim()

            if (isValidDetail(candidate)) {
                return candidate.uppercase()
            }
        }

        detailsRegexTwo.findAll(text).forEach { match ->
            val candidate = match.groupValues[2].trim()

            if (isValidDetail(candidate)) {
                return candidate.uppercase()
            }
        }

        return sender
    }

    private fun extractPaidFrom(text: String): String {
        cardRegex.find(text)?.let {
            return "Card ${it.groupValues[2]}"
        }

        accountRegex.find(text)?.let {
            return "Bank A/C ${it.groupValues[2].takeLast(4)}"
        }

        amazonPayRegex.find(text)?.let {
            return "Wallet"
        }

        return ""
    }

    private fun extractCategory(text: String): ExpenseCategory {
        for ((category, keywords) in categoryKeywords) {
            if (keywords.any { keyword -> text.contains(keyword) }) {
                return category
            }
        }

        return ExpenseCategory.MISC
    }

    private fun generateFingerprint(
        sender: String,
        body: String,
        timestamp: Long
    ): String {
        val input = "$sender|$timestamp|${body.take(100)}"
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
