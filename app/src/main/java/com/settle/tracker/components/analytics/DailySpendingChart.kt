package com.settle.tracker.components.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.settle.tracker.components.analytics.personal.getHighestSpendingWeekday
import com.settle.tracker.components.analytics.personal.getLowestSpendingWeekday
import com.settle.tracker.components.analytics.personal.prepareDailyData
import com.settle.tracker.components.chart.BarChart
import com.settle.tracker.components.chart.BarData
import com.settle.tracker.components.chart.ReferenceLineConfig
import com.settle.tracker.scheme.ExpenseScheme
import com.settle.tracker.ui.theme.Success
import com.settle.tracker.utils.formatCurrency

enum class DailyFilterList(
    val daysCount: Int
) {
    WEEK(7),
    MONTH(30);

    fun getDisplayName() = "Last " + this.daysCount.toString() + " days"
}

@Composable
fun DailySpendingChart(
    expenses: List<ExpenseScheme>
) {
    if (expenses.size <= 1) return

    var selectedFilter by remember { mutableStateOf(DailyFilterList.WEEK) }
    val dailyData =
        remember(expenses, selectedFilter) { prepareDailyData(expenses, selectedFilter.daysCount) }

    if (dailyData.isEmpty()) return

    val averageAmount = dailyData.filter { it.amount != 0.0 }.map { it.amount }.average()
    val recentAverage = dailyData
        .takeLast(7)
        .map { it.amount }
        .average()
    val lowSpendingStreak = dailyData
        .asReversed()
        .takeWhile { it.amount < recentAverage }
        .size

    val highestSpendingDay = remember(dailyData) {
        getHighestSpendingWeekday(dailyData)
    }

    val lowestSpendingDay = remember(dailyData) {
        getLowestSpendingWeekday(dailyData)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(DailyFilterList.entries, key = { it.name }) { filterValue ->
                FilterChip(
                    selected = selectedFilter == filterValue,
                    onClick = { selectedFilter = filterValue },
                    label = { Text(filterValue.getDisplayName()) }
                )
            }
        }

        BarChart(
            modifier = Modifier.height(250.dp),
            xAxisData = dailyData.map { it.date },
            bars = dailyData.map {
                BarData(
                    barValues = listOf(it.amount.toFloat()),
                    barColors = listOf(MaterialTheme.colorScheme.primary)
                )
            },
            tooltipTextStyle = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            ),
            tooltipFormatter = { formatCurrency(it.toDouble()) },
            referenceLine = ReferenceLineConfig(
                referenceLineValue = averageAmount.toFloat(),
                referenceLineColor = MaterialTheme.colorScheme.error,
                referenceLineLabel = "",
                referenceLineTextStyle = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
        )

        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.MonetizationOn,
                    contentDescription = "Spent Today",
                )

                Text(
                    text = "You spent ${formatCurrency(dailyData.last().amount)} today",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            if (lowSpendingStreak > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocalFireDepartment,
                        contentDescription = "Spent Today",
                        tint = Color(0xFFF27D0C)
                    )

                    Text(
                        text = "$lowSpendingStreak low-spending days in a row",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            if (highestSpendingDay != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                        contentDescription = "Spent Today",
                        tint = MaterialTheme.colorScheme.error
                    )

                    Text(
                        text = buildAnnotatedString {
                            append("You spend ")

                            withStyle(
                                style = SpanStyle(
                                    color = MaterialTheme.colorScheme.error
                                )
                            ) {
                                append("most")
                            }

                            append(" on ")

                            withStyle(
                                style = SpanStyle(
                                    color = MaterialTheme.colorScheme.error
                                )
                            ) {
                                append("${highestSpendingDay.first}s")
                            }
                        },
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            if (lowestSpendingDay != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                        contentDescription = "Spent Today",
                        tint = Success
                    )

                    Text(
                        text = buildAnnotatedString {
                            append("You spend ")

                            withStyle(
                                style = SpanStyle(
                                    color = Success
                                )
                            ) {
                                append("least")
                            }

                            append(" on ")

                            withStyle(
                                style = SpanStyle(
                                    color = Success
                                )
                            ) {
                                append("${lowestSpendingDay.first}s")
                            }
                        },
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}
