package com.settle.tracker

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull

import com.settle.tracker.sms.SmsParse

class SmsParserTest {
    val samples = listOf(
        "Sent Rs.18836.00\n" +
                "From HDFC Bank A/C *8161\n" +
                "To CRED Club\n" +
                "On 27/12/25\n" +
                "Ref 572727571825\n" +
                "Not You?\n" +
                "Call 18002586161/SMS BLOCK UPI to 7308080808",
        "Txn Rs.294.00\n" +
                "On HDFC Bank Card 0971\n" +
                "At paytm-blinkit@ptybl \n" +
                "by UPI 536041668348\n" +
                "On 26-12\n" +
                "Not You?\n" +
                "Call 18002586161/SMS BLOCK CC 0971 to 7308080808",
        "INR 1,331.00 spent using ICICI Bank Card XX0008 on 07-Jan-26 on PlayStation Net. Avl Limit: INR 2,02,901.90. If not you, call 1800 2662/SMS BLOCK 0008 to 9215676766.",
        "A/c *9889 Debited for Rs:10000.00 on 12-01-2026 19:05:22 by Mob Bk ref no 601282752712 Avl Bal Rs:2254.51.If not you, Call 1800222243 -Union Bank of India",
        "Payment of Rs 340.00 using Apay Balance successful at merchant. Updated Balance is Rs 5478.28 - If not u? call 180012001637 - SMS via Juspay",
        "Your Apay Wallet balance is debited for INR 904.00. Reference Number is 601382205052.If not u? call 180012001637 - SMS via Juspay\n",
        "Rs.336.00 spent on your SBI Credit Card ending 3783 at RKKAAGRAROADKALYAN on 25/10/25. Trxn. not done by you? Report at https://sbicard.com/Dispute"
    )

    @Test
    fun testHdfcUPI() {
        val expense = SmsParse.parse(
            "AD-HDFCBK-S",
            samples[0],
            System.currentTimeMillis()
        )

        assertNotNull(expense)

        expense?.let {
            assertEquals(expense.details, "CRED".uppercase())
            assertEquals(expense.amount, 18836.00, 0.01)
            assertEquals(expense.paidFrom, "Bank A/C 8161")
        }
    }

    @Test
    fun testHdfcRupay() {
        val expense = SmsParse.parse(
            "AD-HDFCBK-S",
            samples[1],
            System.currentTimeMillis()
        )

        assertNotNull(expense)

        expense?.let {
            assertEquals(expense.details, "paytm-blinkit@ptybl".uppercase())
            assertEquals(expense.amount, 294.00, 0.01)
            assertEquals(expense.paidFrom, "Card 0971")
        }
    }

    @Test
    fun testIciciCard() {
        val expense = SmsParse.parse(
            "AM-ICICI-S",
            samples[2],
            System.currentTimeMillis()
        )

        assertNotNull(expense)

        expense?.let {
            assertEquals(expense.details, "PlayStation".uppercase())
            assertEquals(expense.amount, 1331.00, 0.01)
            assertEquals(expense.paidFrom, "Card 0008")
        }
    }

    @Test
    fun testUnionUPI() {
        val expense = SmsParse.parse(
            "JX-UNIONB-S",
            samples[3],
            System.currentTimeMillis()
        )

        assertNotNull(expense)

        expense?.let {
            assertEquals(expense.details, "".uppercase())
            assertEquals(expense.amount, 10000.00, 0.01)
            assertEquals(expense.paidFrom, "Bank A/C 9889")
        }
    }

    @Test
    fun testAmazonPayFirst() {
        val expense = SmsParse.parse(
            "JUSPAY",
            samples[4],
            System.currentTimeMillis()
        )

        assertNotNull(expense)

        expense?.let {
            assertEquals(expense.details, "MERCHANT".uppercase())
            assertEquals(expense.amount, 340.00, 0.01)
            assertEquals(expense.paidFrom, "Wallet")
        }
    }

    @Test
    fun testAmazonPaySecond() {
        val expense = SmsParse.parse(
            "JUSPAY",
            samples[5],
            System.currentTimeMillis()
        )

        assertNotNull(expense)

        expense?.let {
            assertEquals(expense.details, "".uppercase())
            assertEquals(expense.amount, 904.00, 0.01)
            assertEquals(expense.paidFrom, "Wallet")
        }
    }

    @Test
    fun testSBICard() {
        val expense = SmsParse.parse(
            "AD-SBIB-S",
            samples[6],
            System.currentTimeMillis()
        )

        assertNotNull(expense)

        expense?.let {
            assertEquals(expense.details, "RKKAAGRAROADKALYAN".uppercase())
            assertEquals(expense.amount, 336.00, 0.01)
            assertEquals(expense.paidFrom, "Card 3783")
        }
    }
}
