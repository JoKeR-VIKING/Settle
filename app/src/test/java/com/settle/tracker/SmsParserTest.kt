package com.settle.tracker

import com.settle.tracker.sms.SmsParse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

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
        "Rs.336.00 spent on your SBI Credit Card ending 3783 at RKKAAGRAROADKALYAN on 25/10/25. Trxn. not done by you? Report at https://sbicard.com/Dispute",
        "Important Update: HDFC Bank Card xx0971:\n" +
                "Higher Rs. 1500000 Loan on Card at lower interest rate 0.84%. Check EMIs.\n" +
                "https://hdfcbk.io/HDFCBK/s/XVwooJGg\n" +
                "T&C",
        "ICICI Bank Acct XX552 debited for Rs 370.00 on 19-Sep-26; Mangalam shoes credited. " +
                "UPI:626236224713. Call 18002662 for dispute. SMS BLOCK 552 to 9215676766."
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
            assertEquals(expense.details, "CRED Club".uppercase())
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
            assertEquals(expense.details, "JX-UNIONB-S")
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
            assertEquals(expense.details, "JUSPAY")
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
            assertEquals(expense.details, "RKKAAGRAROADKALYAN")
            assertEquals(expense.amount, 336.00, 0.01)
            assertEquals(expense.paidFrom, "Card 3783")
        }
    }

    @Test
    fun testSpamSMS() {
        val expense = SmsParse.parse(
            "AD-HDFCBK-S",
            samples[7],
            System.currentTimeMillis()
        )

        assertNull(expense)
    }

    @Test
    fun testOtpIsNotBooked() {
        val expense = SmsParse.parse(
            "AD-HDFCBK-S",
            "123456 is your OTP for a transaction of Rs.500 at AMAZON. Do not share it with anyone.",
            System.currentTimeMillis()
        )

        assertNull(expense)
    }

    @Test
    fun testCardBillIsNotBooked() {
        val expense = SmsParse.parse(
            "AD-HDFCBK-S",
            "Your Credit Card statement for Rs.12,345.00 is generated. Total amount due: Rs.12,345.00. Minimum amount due: Rs.1,200.00. Due date: 05-Feb-26",
            System.currentTimeMillis()
        )

        assertNull(expense)
    }

    @Test
    fun testUpcomingEmiIsNotBooked() {
        val expense = SmsParse.parse(
            "AD-HDFCBK-S",
            "Rs.649.00 will be debited towards your NETFLIX standing instruction on 15-Feb-26.",
            System.currentTimeMillis()
        )

        assertNull(expense)
    }

    @Test
    fun testCompletedStandingInstructionIsBooked() {
        val expense = SmsParse.parse(
            "AD-HDFCBK-S",
            "Rs.649.00 debited towards NETFLIX standing instruction from HDFC Bank A/c XX1234. Avl Bal Rs.5,000.00",
            System.currentTimeMillis()
        )

        assertNotNull(expense)

        expense?.let {
            assertEquals(expense.amount, 649.00, 0.01)
        }
    }

    @Test
    fun testBalanceIsNeverBookedAsTheSpend() {
        val expense = SmsParse.parse(
            "AD-HDFCBK-S",
            "A/C X1234 debited by 35.0 on 01-Jan-26. Avl Bal Rs 800.50",
            System.currentTimeMillis()
        )

        assertNotNull(expense)

        expense?.let {
            assertEquals(expense.amount, 35.00, 0.01)
        }
    }

    @Test
    fun testIciciAcctNoPreposition() {
        val expense = SmsParse.parse(
            "AM-ICICI-S",
            samples[8],
            System.currentTimeMillis()
        )

        assertNotNull(expense)

        expense?.let {
            assertEquals(expense.details, "Mangalam shoes".uppercase())
            assertEquals(expense.amount, 370.00, 0.01)
            assertEquals(expense.paidFrom, "Bank A/C 552")
        }
    }

    @Test
    fun testBareNumberSenderIsUntrusted() {
        val expense = SmsParse.parse(
            "+919876543210",
            "Rs.500 debited from A/c XX1234. If not you, click bit.ly/xyz",
            System.currentTimeMillis()
        )

        assertNull(expense)
    }
}
