package com.settle.tracker.sms

/**
 * Deterministic SMS body classifier, ported from abhirajsinha/omoi-sms-parser
 * (src/parser.ts). Pure — no I/O, no clock. This is Layer 1 ("what does the
 * body say"); [SenderClassifier] is Layer 0 ("who is allowed to say it").
 *
 * `classifySms` sorts every message into exactly one class, first match wins,
 * and the order is the safety net:
 *   OTP -> hard non-financial -> card bill -> reversal -> upcoming (future
 *   tense) -> credit -> debit -> informational (balance/limit) -> financial
 *   but unreadable -> non-financial.
 *
 * The two bugs the upstream project fixed, and that this port inherits the fix
 * for:
 *   - amounts are matched leftmost-across-all-patterns, not first-pattern-wins,
 *     after balance/limit clauses are masked out of the search space — so a
 *     trailing "Avl Bal Rs 800.50" can never be booked as the spend.
 *   - "will be debited" (an EMI/SIP pre-notice) is told apart from a completed
 *     debit by verb TENSE, never by vocabulary, because RBI's e-mandate
 *     pre-notice and the debit it precedes share the same wording otherwise.
 */
object SmsGrammar {

    sealed class SmsClass {
        data class Debit(val amountCents: Long, val merchantRaw: String?, val refId: String?) : SmsClass()
        data object Credit : SmsClass()
        data class CardBill(val totalDueCents: Long?, val minDueCents: Long?, val dueDate: String?) : SmsClass()
        data class Reversal(val amountCents: Long?, val refId: String?) : SmsClass()
        data object Upcoming : SmsClass()
        data object FinancialUnparsed : SmsClass()
        data object NonFinancial : SmsClass()
    }

    /**
     * ₹5,00,000 — anything above this is far likelier to be a mis-read
     * (a balance, a credit limit, a loan offer) than a real consumer debit.
     */
    const val AMOUNT_CAP_CENTS = 500_000_00L

    private fun ci(pattern: String) = Regex(pattern, RegexOption.IGNORE_CASE)

    private val otpPatterns = listOf(
        ci("""\b\d{3,8}\s+is\s+(?:the\s+|your\s+|an?\s+)?(?:otp|one[- ]time\s+password|verification\s+code)\b"""),
        ci("""\b(?:otp|one[- ]time\s+password|verification\s+code)\b[^\n]{0,60}?(?:\bis\b|:)\s*\d{3,8}\b"""),
        ci("""\b(?:use|enter)\s+(?:otp|one[- ]time\s+password)\s*:?\s*\d{3,8}\b""")
    )

    private val nonFinancialPatterns = listOf(
        ci("""\b(?:cashback|coupon|voucher|discount|pre[- ]?approved|lucky draw)\b"""),
        ci("""\b(?:declined|failed|unsuccessful)\b"""),
        ci("""\b(?:returned unpaid|dishonou?red|bounced)\b"""),
        ci("""\b(?:fixed deposit|matures on|maturity date)\b"""),
        ci("""(?:rs\.?|inr|₹)\s?[\d,]+(?:\.\d{1,2})?\s*(?:off|cashback)\b"""),
        ci("""\b(?:flat|upto|up to|at just|starting at|only)\s+(?:rs\.?|inr|₹)"""),
        ci("""\b(?:shop now|buy now|claim now|t&c apply|terms and conditions apply)\b"""),
        ci("""\b(?:congratulations|you have won|you've won)\b"""),
        ci("""\bspent\b[^\n]{0,80}\bthis\s+(?:month|week|year)\b"""),
        ci("""\bspend\s+(?:analysis|summary|report|insights)\b"""),
        ci("""\bin the (?:last|past)\s+\d+\s+days\b""")
    )

    private val cardBillPatterns = listOf(
        ci("""\bstatement\b"""),
        ci("""\b(?:total|min(?:imum)?)\s+(?:amount\s+)?due\b"""),
        ci("""\bbill\s+(?:has been\s+)?generated\b"""),
        ci("""\bmin(?:imum)?\s+amt\b""")
    )

    private val totalDuePatterns = listOf(
        ci("""\btotal\s+(?:amount\s+)?due\b\s*(?:is)?\s*[:\-]?\s*(?:rs\.?|inr|₹)\s*[:.]?\s*([\d,]+(?:\.\d{1,2})?)"""),
        ci("""\bamount\s+due\b\s*(?:is)?\s*[:\-]?\s*(?:rs\.?|inr|₹)\s*[:.]?\s*([\d,]+(?:\.\d{1,2})?)"""),
        ci("""\btotal\s+payment\s+of\s+(?:rs\.?|inr|₹)\s*[:.]?\s*([\d,]+(?:\.\d{1,2})?)""")
    )

    private val minDuePatterns = listOf(
        ci("""\bmin(?:imum)?\s+(?:amount\s+)?due\b\s*(?:is)?\s*[:\-]?\s*(?:rs\.?|inr|₹)\s*[:.]?\s*([\d,]+(?:\.\d{1,2})?)"""),
        ci("""\bmin(?:imum)?\s+amt\s+(?:of\s+)?(?:rs\.?|inr|₹)\s*[:.]?\s*([\d,]+(?:\.\d{1,2})?)""")
    )

    private val dueDatePatterns = listOf(
        ci("""\bdue\s*(?:date|by|on)\b\s*[:\-]?\s*(\d{1,2}[-/ ][A-Za-z0-9]{2,4}[-/ ]\d{2,4})"""),
        ci("""\bby\s+(\d{1,2}[-/ ][A-Za-z0-9]{2,4}[-/ ]\d{2,4})""")
    )

    private val reversalPatterns = listOf(
        ci("""\brevers(?:ed|al)\b""")
    )

    private val futureDebitPatterns = listOf(
        ci("""\b(?:will|would|shall)\s+be\s+(?:debited|deducted|auto[- ]?debited|charged)\b"""),
        ci("""\b(?:will|would|shall)\s+(?:be\s+)?(?:auto[- ]?debit|debit|deduct)\b""")
    )

    private val upcomingPatterns = futureDebitPatterns + listOf(
        ci("""\b(?:reminder|due on|is due|scheduled for)\b""")
    )

    private val creditPatterns = listOf(
        ci("""\bcredited\b"""),
        ci("""\brefund(?:ed)?\b"""),
        ci("""\breceived\b""")
    )

    private val debitVerbs = listOf(
        ci("""\bdebited\b"""),
        ci("""\bwithdrawn\b"""),
        ci("""\bdebit\s+by\s+transfer\b"""),
        ci("""(?:rs\.?|inr|₹)\s?[\d,]+(?:\.\d{1,2})?\s+(?:\w+\s+){0,3}(?:spent|paid)\b"""),
        ci("""\b(?:sent|paid)\s+(?:rs\.?|inr|₹)"""),
        ci("""\bspent\s+card\s+(?:no\.?|number)?\s*[Xx*\d]"""),
        ci("""\b(?:has been|was)\s+used\s+for\s+an?\s+(?:transaction|purchase)\s+of\s+(?:rs\.?|inr|₹)"""),
        // Wallet/gateway grammar (Amazon Pay via Juspay, etc.): "Payment of
        // Rs.340.00 using Apay Balance successful at merchant." — these never
        // say "spent"/"debited"; the bank-only upstream grammar misses them.
        ci("""\bpayment\s+of\s+(?:rs\.?|inr|₹)\s*[:.]?\s*[\d,]+(?:\.\d{1,2})?\s+using\b""")
    )

    private val debitHints = debitVerbs + listOf(ci("""\btxn\b"""))

    private val informationalPatterns = listOf(
        ci("""\b(?:avl|available)\s*(?:bal|balance|lmt|limit)\b"""),
        ci("""\bbalance\s+is\b"""),
        ci("""\bcredit\s+limit\b"""),
        ci("""\b(?:reward\s+points|points\s+worth|bonus\s+points)\b""")
    )

    private val amountPatterns = listOf(
        // Some issuers (Union Bank) use "Rs:" instead of "Rs." or "Rs ".
        ci("""(?:Rs\.?|INR|₹)\s*[:.]?\s*([\d,]+(?:\.\d{1,2})?)"""),
        // SBI UPI omits the currency token entirely: "A/c debited by 420.50".
        ci("""\bdebited by\s+([\d,]+(?:\.\d{1,2})?)"""),
        // HDFC reversal omits it too.
        ci("""\btransaction of\s+([\d,]+(?:\.\d{1,2})?)""")
    )

    /**
     * Balance/limit clauses — excised (masked to spaces, same length) from the
     * search space before any amount pattern runs, so a balance can never be
     * mistaken for the transaction amount.
     */
    private val balanceClausePatterns = listOf(
        ci("""\b(?:avl|avbl|available|total|updated|remaining)\.?\s*(?:credit\s+)?(?:bal(?:ance)?|lmt|limit)\b[^\d\n]{0,30}(?:rs\.?|inr|₹)?\s*[\d,]+(?:\.\d{1,2})?"""),
        // Filler between "balance is/:" and the amount is deliberately narrow
        // (whitespace/currency only) — a wallet debit alert like "Wallet
        // balance is debited for INR 904.00" must NOT be read as a balance
        // clause, or the real debit amount gets excised along with it.
        ci("""\b(?:balance|credit\s+limit)\s*(?:is|:)\s*(?:rs\.?|inr|₹)?\s*[\d,]+(?:\.\d{1,2})?"""),
        ci("""\boutstanding\b[^\d\n]{0,20}(?:rs\.?|inr|₹)?\s*[\d,]+(?:\.\d{1,2})?""")
    )

    private val refPatterns = listOf(
        ci("""\bref(?:erence)?\.?\s*(?:no|number)?\.?\s*:?\s*([A-Z]{0,5}\d{9,16})\b"""),
        ci("""\butr\.?\s*(?:no|number)?\.?\s*:?\s*([A-Z]{0,5}\d{9,16})\b""")
    )

    private val merchantPatterns = listOf(
        // UPI VPA — "to VPA swiggy@ybl", "credited to zomato@paytm". Local part only.
        ci("""\b(?:to|towards)\s+(?:VPA\s+)?([A-Za-z0-9][A-Za-z0-9._-]*@[A-Za-z][A-Za-z0-9.]*)"""),
        // Axis/ICICI "Info- UPI/P2M/123456/SWIGGY." / "Info: CITY GROCERS RETAIL."
        ci("""\bInfo\b\s*[:\-]\s*([^.;\n]+)"""),
        // ICICI "Acct XX552 debited for Rs 370.00 on 19-Sep-26; Mangalam shoes
        // credited." — the beneficiary is named directly before "credited",
        // with no "at/to" preposition at all.
        ci("""[;.,]\s*([A-Za-z][A-Za-z0-9 .&'-]{1,40}?)\s+credited\b"""),
        // "at AMAZON on …", "to SWIGGY Refno …" — must start with a letter so a
        // clock ("at 12:33:12") is never read as a shop.
        ci("""\b(?:at|to|towards)\s+([A-Za-z][A-Za-z0-9 .&'-]{1,40}?)(?=\s+(?:on|dated|ref|refno|upi|avl)\b|[.;,:(\n]|$)""")
    )

    /** Rupee string -> integer paise via string arithmetic (never a Double). */
    fun toPaise(raw: String): Long {
        val cleaned = raw.replace(",", "")
        val dot = cleaned.indexOf('.')
        val rupees = (if (dot >= 0) cleaned.substring(0, dot) else cleaned).toLongOrNull() ?: 0L
        val paiseRaw = if (dot >= 0) cleaned.substring(dot + 1) else ""
        val paiseStr = paiseRaw.take(2).padEnd(2, '0')
        val paise = paiseStr.toLongOrNull() ?: 0L
        return rupees * 100 + paise
    }

    private fun firstMatch(patterns: List<Regex>, text: String): String? {
        for (re in patterns) {
            val m = re.find(text)
            val value = m?.groups?.get(1)?.value
            if (!value.isNullOrEmpty()) return value
        }
        return null
    }

    private data class PositionedMatch(val value: String, val index: Int)

    /** Position decides, not pattern order — used only for money. */
    private fun leftmostMatch(patterns: List<Regex>, text: String): PositionedMatch? {
        var best: PositionedMatch? = null
        for (re in patterns) {
            val m = re.find(text)
            val value = m?.groups?.get(1)?.value
            if (!value.isNullOrEmpty()) {
                val index = m!!.range.first
                if (best == null || index < best.index) best = PositionedMatch(value, index)
            }
        }
        return best
    }

    private fun any(patterns: List<Regex>, text: String): Boolean = patterns.any { it.containsMatchIn(text) }

    /**
     * Blanks out every balance/limit clause, preserving length, so indices
     * into the result stay valid indices into the original body.
     */
    fun maskBalances(text: String): String {
        var masked = text
        for (re in balanceClausePatterns) {
            masked = re.replace(masked) { m -> " ".repeat(m.value.length) }
        }
        return masked
    }

    /** Drops "will be debited" clauses so a pre-notice cannot look like a debit. */
    private fun stripFutureClauses(text: String): String {
        var stripped = text
        for (re in futureDebitPatterns) {
            stripped = re.replace(stripped, " ")
        }
        return stripped
    }

    private data class AmountMatch(val cents: Long, val index: Int)

    private fun amountAt(text: String): AmountMatch? {
        val m = leftmostMatch(amountPatterns, maskBalances(text)) ?: return null
        val cents = toPaise(m.value)
        return if (cents > 0) AmountMatch(cents, m.index) else null
    }

    /**
     * The merchant is named in the clause that follows the amount, so only
     * look after it — otherwise prose ahead of the amount ("We would like to
     * inform you that Rs. 1284.35 …") gets read as the merchant.
     */
    private fun merchantOf(fullText: String, amountIndex: Int): String? {
        val text = fullText.substring(amountIndex)
        val merchant = firstMatch(merchantPatterns, text) ?: return null
        val cleaned = if (merchant.contains("@")) merchant.substringBefore("@") else merchant
        return cleaned.trim().ifEmpty { null }
    }

    /** Every message lands in exactly one class. Nothing is silently dropped. */
    fun classifySms(body: String): SmsClass {
        val text = body.trim()
        if (text.isEmpty()) return SmsClass.NonFinancial

        // 1. OTP first — it quotes the amount/merchant of a purchase not yet
        // debited. Booking it would double-count the real charge.
        if (any(otpPatterns, text)) return SmsClass.NonFinancial

        // 2. Bodies that can never be a completed debit.
        if (any(nonFinancialPatterns, text)) return SmsClass.NonFinancial

        // 3. Card bill / statement — every underlying swipe already booked.
        if (any(cardBillPatterns, text)) {
            val total = firstMatch(totalDuePatterns, text)
            val min = firstMatch(minDuePatterns, text)
            return SmsClass.CardBill(
                totalDueCents = total?.let { toPaise(it) },
                minDueCents = min?.let { toPaise(it) },
                dueDate = firstMatch(dueDatePatterns, text)
            )
        }

        // 4. A debit that came back.
        if (any(reversalPatterns, text)) {
            return SmsClass.Reversal(
                amountCents = amountAt(text)?.cents,
                refId = firstMatch(refPatterns, text)
            )
        }

        // A debit verb that survives removal of "will be …" is a COMPLETED debit.
        val completed = stripFutureClauses(text)
        val isDebitDirection = any(debitVerbs, completed)

        // 5. A debit that hasn't happened yet.
        if (!isDebitDirection && any(upcomingPatterns, text)) return SmsClass.Upcoming

        // 6. Incoming money — only when nothing settles the direction outward.
        if (!isDebitDirection && any(creditPatterns, text)) return SmsClass.Credit

        // 7. The debit parse — the only branch that can create an expense.
        val amount = amountAt(text)

        // The cap: an amount this large is a mis-read far more often than real.
        if (amount != null && amount.cents > AMOUNT_CAP_CENTS) return SmsClass.FinancialUnparsed

        if (amount != null && (isDebitDirection || any(debitHints, completed))) {
            return SmsClass.Debit(
                amountCents = amount.cents,
                merchantRaw = merchantOf(text, amount.index),
                refId = firstMatch(refPatterns, text)
            )
        }

        // 8. A balance/limit amount — checked AFTER the debit parse, since real
        // debit alerts routinely carry an "Avl Bal" line too.
        if (any(informationalPatterns, text)) return SmsClass.NonFinancial

        // 9. It IS financial, the grammar just can't read it — never guessed.
        if (amount != null) return SmsClass.FinancialUnparsed

        return SmsClass.NonFinancial
    }
}
