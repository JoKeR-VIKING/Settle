package com.settle.tracker.components.analytics.personal

import android.icu.util.Calendar
import com.settle.tracker.scheme.ExpenseScheme
import java.text.SimpleDateFormat
import java.util.Locale

data class MonthlyPoints(
    val monthName: String,
    val amount: Double
)

fun prepareMonthlyData(expenses: List<ExpenseScheme>): List<MonthlyPoints> {
    val calendar = Calendar.getInstance()
    val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())

    return expenses
        .groupBy {
            calendar.timeInMillis = it.timestamp
            "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH)}"
        }
        .map { (_, monthlyList) ->
            calendar.timeInMillis = monthlyList.first().timestamp
            MonthlyPoints(
                monthName = monthFormat.format(calendar.time),
                amount = monthlyList.sumOf { it.amount }
            )
        }
        .sortedByDescending { it.monthName }
        .takeLast(12)
}
