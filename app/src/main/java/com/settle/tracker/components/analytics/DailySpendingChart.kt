package com.settle.tracker.components.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.himanshoe.charty.bar.BarChart
import com.himanshoe.charty.bar.config.BarChartConfig
import com.himanshoe.charty.bar.data.BarData
import com.himanshoe.charty.color.ChartyColor
import com.himanshoe.charty.common.config.Animation
import com.himanshoe.charty.common.config.ChartScaffoldConfig
import com.himanshoe.charty.common.config.CornerRadius
import com.himanshoe.charty.common.config.ReferenceLineConfig
import com.himanshoe.charty.common.tooltip.TooltipConfig
import com.settle.tracker.components.analytics.personal.getHighestSpendingWeekday
import com.settle.tracker.components.analytics.personal.getLowestSpendingWeekday
import com.settle.tracker.components.analytics.personal.prepareDailyData
import com.settle.tracker.scheme.ExpenseScheme
import com.settle.tracker.utils.formatCurrency

@Composable
fun DailySpendingChart(
    expenses: List<ExpenseScheme>
) {
    if (expenses.size <= 1) return

    val dailyData = remember(expenses) { prepareDailyData(expenses) }
    val listState = rememberLazyListState()

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

    LaunchedEffect(dailyData.size) {
        listState.scrollToItem(0, scrollOffset = Int.MAX_VALUE)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(start = 20.dp)
        ) {
            item {
                BarChart(
                    modifier = Modifier
                        .width((dailyData.size * 60).dp)
                        .height(200.dp),
                    data = {
                        dailyData.map { point ->
                            BarData(
                                label = point.date,
                                value = point.amount.toFloat()
                            )
                        }
                    },
                    color = ChartyColor.Solid(MaterialTheme.colorScheme.primary),
                    barConfig = BarChartConfig(
                        barWidthFraction = 0.6f,
                        cornerRadius = CornerRadius.ExtraLarge,
                        animation = Animation.Enabled(),
                        referenceLine = ReferenceLineConfig(
                            value = averageAmount.toFloat(),
                            label = "",
                            labelTextStyle = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            labelOffset = 10f
                        ),
                        tooltipConfig = TooltipConfig(
                            backgroundColor = MaterialTheme.colorScheme.secondary
                        ),
                        tooltipFormatter = { lineData ->
                            formatCurrency(lineData.value.toDouble())
                        }
                    ),
                    onBarClick = {},
                    scaffoldConfig = ChartScaffoldConfig(
                        axisColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        labelTextStyle = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                )
            }
        }

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
                        tint = MaterialTheme.colorScheme.surfaceBright
                    )

                    Text(
                        text = buildAnnotatedString {
                            append("You spend ")

                            withStyle(
                                style = SpanStyle(
                                    color = MaterialTheme.colorScheme.surfaceBright
                                )
                            ) {
                                append("least")
                            }

                            append(" on ")

                            withStyle(
                                style = SpanStyle(
                                    color = MaterialTheme.colorScheme.surfaceBright
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
