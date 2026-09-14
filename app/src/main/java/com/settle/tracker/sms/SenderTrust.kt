package com.settle.tracker.sms

/**
 * Ported from the sender-trust gate in abhirajsinha/omoi-sms-parser (src/sender.ts).
 *
 * No regex over an SMS *body* can tell a real bank alert from phishing, because
 * phishing copies the real alert verbatim. Only the DLT-registered *sender*
 * header can. Indian A2P SMS headers look like `VM-HDFCBK-S` (or the legacy
 * no-hyphen `HDFCBK`); the middle segment is the sender's registered identity,
 * the trailing letter (T/S/P/G) is stamped by the telco, not the sender.
 */
enum class SenderTrust {
    /** A DLT header in a known bank/issuer family. */
    BANK,

    /** A valid DLT header that isn't a recognised bank (wallet, gateway, merchant). */
    COMMERCIAL,

    /** A bare phone number or a `-P` promotional header — the phishing class. */
    UNTRUSTED,

    /** No sender on the wire. */
    ABSENT
}

object SenderClassifier {
    private val bareNumber = Regex("""^\+?[\d\s]{7,}$""")
    private val dltHyphenated = Regex("""^[A-Z]{2}-[A-Z0-9]{3,9}(?:-[STPG])?$""", RegexOption.IGNORE_CASE)

    // Deliberately uppercase-only: DLT headers are registered uppercase.
    private val dltLegacy = Regex("""^[A-Z]{2}[A-Z0-9]{4,7}$""")

    /**
     * Bank/issuer header families — a bounded prefix match (see [bankHeader]).
     * No 3-char stubs: a short stub can promote an unrelated sender (e.g. a
     * courier) to BANK.
     */
    val BANK_HEADER_FAMILIES: List<String> = listOf(
        "HDFC",
        "PAYZAP", // HDFC's PayZapp
        "ICICI",
        "ICBANK",
        "SBI", // SBIUPI / SBIINB / SBICRD / SBIBNK
        "ATMSBI",
        "CBSSBI",
        "AXIS",
        "AXSFIN",
        "KOTAK",
        "KTKREM",
        "IDFC",
        "YESBNK",
        "YESBK",
        "INDUS",
        "PNBSMS",
        "PNBINB",
        "BOBSMS",
        "BOBTXN",
        "BOBIBK",
        "CANBNK",
        "CAANBK",
        "UNION",
        "FEDBNK",
        "FEDBK",
        "RBLBNK",
        "RBLCRD",
        "AUBANK",
        "PAYTMB", // Paytm Payments BANK — not the wallet (PAYTMW/PYTM…)
    )

    private val bankHeader = Regex("^(?:${BANK_HEADER_FAMILIES.joinToString("|")})[A-Z0-9]{0,3}$")

    fun classify(sender: String?): SenderTrust {
        val s = sender?.trim()
        if (s.isNullOrEmpty()) return SenderTrust.ABSENT

        if (bareNumber.matches(s)) return SenderTrust.UNTRUSTED

        val hyphenated = dltHyphenated.matches(s)
        if (!hyphenated && !dltLegacy.matches(s)) return SenderTrust.UNTRUSTED

        val parts = s.uppercase().split("-")
        // `-P` is stamped by the telco: this traffic is promotional and can
        // never be a debit, whatever it says.
        if (parts.size == 3 && parts[2] == "P") return SenderTrust.UNTRUSTED

        val headers = if (hyphenated) listOf(parts[1]) else listOf(parts[0], parts[0].drop(2))

        return if (headers.any { bankHeader.matches(it) }) SenderTrust.BANK else SenderTrust.COMMERCIAL
    }

    /** Only a verified bank/issuer sender may book an expense with no review. */
    fun canBook(trust: SenderTrust): Boolean = trust == SenderTrust.BANK
}
