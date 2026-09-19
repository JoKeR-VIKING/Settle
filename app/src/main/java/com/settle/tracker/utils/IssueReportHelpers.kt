package com.settle.tracker.utils

private val SMS_RELATED_KEYWORDS = listOf(
    "sms", "bank", "parse", "parsing", "not detected", "not working",
    "icici", "hdfc", "sbi", "axis", "kotak", "idfc", "yes bank", "indus",
    "pnb", "bob", "canara", "union bank", "federal bank", "rbl", "au bank",
    "upi", "paid from", "merchant", "account number", "card ending",
    "expense not added", "wrong amount", "missed expense"
)

/** Whether this report likely concerns SMS-based expense detection. */
fun mentionsSmsIssue(subject: String, summary: String): Boolean {
    val text = "$subject $summary".lowercase()
    return SMS_RELATED_KEYWORDS.any { text.contains(it) }
}
