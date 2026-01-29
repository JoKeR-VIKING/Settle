package com.settle.tracker.components.analytics.personal

import android.icu.util.Calendar
import com.settle.tracker.components.chart.PieData
import com.settle.tracker.scheme.ExpenseScheme
import com.settle.tracker.utils.formatTimestamp
import com.settle.tracker.utils.getExpenseCategoryColor
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

data class MonthlyPoints(
    val yearMonthKey: Int,
    val monthName: String,
    val amount: Double
)

data class DailyPoints(
    val timestamp: Long,
    val date: String,
    val amount: Double
)

data class CategoryIncrease(
    val category: String,
    val currentMonthSpend: Double,
    val historicalAverage: Double,
    val percentageIncrease: Double
)

fun Long.toYearMonth(): YearMonth =
    Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .let { YearMonth.of(it.year, it.month) }

fun prepareMonthlyData(expenses: List<ExpenseScheme>): List<MonthlyPoints> {
    val calendar = Calendar.getInstance()

    return expenses
        .groupBy {
            calendar.timeInMillis = it.timestamp
            calendar.get(Calendar.YEAR) * 100 + calendar.get(Calendar.MONTH)
        }
        .map { (yearMonthKey, monthlyList) ->
            calendar.timeInMillis = monthlyList.first().timestamp
            MonthlyPoints(
                yearMonthKey = yearMonthKey,
                monthName = formatTimestamp(calendar.timeInMillis, "MMM"),
                amount = monthlyList.sumOf { it.amount }
            )
        }
        .sortedByDescending { it.yearMonthKey }
        .takeLast(6)
        .reversed()
}

fun prepareDailyData(expenses: List<ExpenseScheme>): List<DailyPoints> {
    val dayFormat = SimpleDateFormat("dd MMM", Locale.ENGLISH)
    val calendar = Calendar.getInstance()

    val dailyExpenseMap = expenses.groupBy {
        calendar.timeInMillis = it.timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        calendar.timeInMillis
    }.mapValues { entry ->
        entry.value.sumOf { it.amount }
    }

    val today = Calendar.getInstance()

    return (0 until 31).map { offset ->
        val dayCal = today.clone() as Calendar
        dayCal.add(Calendar.DAY_OF_YEAR, -offset)

        val dayTimestamp = dayCal.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        DailyPoints(
            timestamp = dayTimestamp,
            date = dayFormat.format(dayTimestamp),
            amount = dailyExpenseMap[dayTimestamp] ?: 0.0
        )
    }.reversed()
}

fun prepareCategorizedData(
    expenses: List<ExpenseScheme>
): List<PieData> {
    val currentMonth = YearMonth.now()

    return expenses
        .filter {
            val expenseMonth = Instant.ofEpochMilli(it.timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .let { date -> YearMonth.of(date.year, date.month) }

            expenseMonth == currentMonth
        }
        .groupBy { it.category }
        .map { (category, list) ->
            PieData(
                label = category,
                value = list.sumOf { it.amount }.toFloat(),
                color = getExpenseCategoryColor(category)
            )
        }
        .sortedByDescending {
            it.value
        }
}

fun getHighestCategorySpend(
    expenses: List<ExpenseScheme>
): List<CategoryIncrease> {
    val currentMonth = YearMonth.now()

    val categoryMonthTotals =
        expenses.groupBy { it.category }
            .mapValues { (_, categoryExpenses) ->
                categoryExpenses
                    .groupBy { it.timestamp.toYearMonth() }
                    .mapValues { (_, monthExpenses) ->
                        monthExpenses.sumOf { it.amount }
                    }
            }

    return categoryMonthTotals
        .mapNotNull { (category, monthMap) ->
            val currentSpend = monthMap[currentMonth] ?: return@mapNotNull null

            val historicalMonths = monthMap
                .filterKeys { it.isBefore(currentMonth) }
                .values

            if (historicalMonths.size < 2) return@mapNotNull null

            val historicalAverage = historicalMonths.average()

            if (historicalAverage == 0.0) return@mapNotNull null

            val percentageIncrease =
                ((currentSpend - historicalAverage) / historicalAverage) * 100

            if (percentageIncrease <= 0) return@mapNotNull null

            CategoryIncrease(
                category = category,
                currentMonthSpend = currentSpend,
                historicalAverage = historicalAverage,
                percentageIncrease = percentageIncrease
            )
        }
        .sortedByDescending { it.percentageIncrease }
}

fun getHighestSpendingWeekday(
    dailyData: List<DailyPoints>
): Pair<String, Double>? {
    if (dailyData.isEmpty()) return null

    val totalsByDay = dailyData.groupBy { point ->
        Instant.ofEpochMilli(point.timestamp)
            .atZone(ZoneId.systemDefault())
            .dayOfWeek
    }.mapValues { (_, list) ->
        list.sumOf { it.amount }
    }

    val maxEntry = totalsByDay.maxByOrNull { it.value } ?: return null

    val dayName = maxEntry.key.getDisplayName(
        TextStyle.FULL,
        Locale.ENGLISH
    )

    return dayName to maxEntry.value
}

fun getLowestSpendingWeekday(
    dailyData: List<DailyPoints>
): Pair<String, Double>? {
    if (dailyData.isEmpty()) return null

    val totalsByDay = dailyData.groupBy { point ->
        Instant.ofEpochMilli(point.timestamp)
            .atZone(ZoneId.systemDefault())
            .dayOfWeek
    }.mapValues { (_, list) ->
        list.sumOf { it.amount }
    }

    val maxEntry = totalsByDay.minByOrNull { it.value } ?: return null

    val dayName = maxEntry.key.getDisplayName(
        TextStyle.FULL,
        Locale.ENGLISH
    )

    return dayName to maxEntry.value
}
