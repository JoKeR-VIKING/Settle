package com.settle.tracker

import com.settle.tracker.utils.mentionsSmsIssue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IssueReportHelpersTest {
    @Test
    fun detectsBankAndSmsKeywords() {
        assertTrue(mentionsSmsIssue("ICICI sms not working", ""))
        assertTrue(mentionsSmsIssue("Expense not added", "The paid from field is blank for HDFC UPI"))
    }

    @Test
    fun ignoresUnrelatedReports() {
        assertFalse(mentionsSmsIssue("App crashes on login", "Happens every time I open the app"))
    }
}
