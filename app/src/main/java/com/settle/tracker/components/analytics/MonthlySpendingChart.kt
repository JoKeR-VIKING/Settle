package com.settle.tracker.components.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.himanshoe.charty.color.ChartyColor
import com.himanshoe.charty.common.config.ChartScaffoldConfig
import com.himanshoe.charty.common.config.ReferenceLineConfig
import com.himanshoe.charty.common.tooltip.TooltipConfig
import com.himanshoe.charty.line.LineChart
import com.himanshoe.charty.line.config.LineChartConfig
import com.himanshoe.charty.line.data.LineData
import com.settle.tracker.components.analytics.personal.prepareMonthlyData
import com.settle.tracker.scheme.ExpenseScheme
import com.settle.tracker.utils.formatCurrency
import com.settle.tracker.utils.toFullMonthName
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

@Composable
fun MonthlySpendingChart(
    expenses: List<ExpenseScheme>
) {
    if (expenses.size <= 1) return

    val monthlyChart = remember(expenses) { prepareMonthlyData(expenses) }

    val averageAmount: Double =
        if (monthlyChart.isNotEmpty()) {
            monthlyChart.map { it.amount }.average()
        } else {
            0.0
        }

    val last = monthlyChart.lastOrNull()?.amount ?: 0.0
    val prev = monthlyChart.dropLast(1).lastOrNull()?.amount ?: 0.0
    val percentageChange =
        if (prev > 0) ((last - prev) / prev) * 100 else 0.0

    val spikeThreshold = averageAmount * 2
    val spikes = monthlyChart.filter { it.amount > spikeThreshold }
    val biggestSpike = spikes.maxByOrNull { it.amount }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        LineChart(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(200.dp),
            data = {
                monthlyChart.map { point ->
                    LineData(
                        label = point.monthName,
                        value = point.amount.toFloat()
                    )
                }
            },
            color = ChartyColor.Solid(MaterialTheme.colorScheme.primary),
            lineConfig = LineChartConfig(
                lineWidth = 2f,
                showPoints = true,
                smoothCurve = false,
                pointRadius = 15f,
                referenceLine = ReferenceLineConfig(
                    value = averageAmount.toFloat(),
                    label = "Average: ${formatCurrency(averageAmount)}",
                    labelTextStyle = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    labelOffset = 10f
                ),
                tooltipConfig = TooltipConfig(
                    backgroundColor = MaterialTheme.colorScheme.primary
                ),
                tooltipFormatter = { lineData ->
                    formatCurrency(lineData.value.toDouble())
                }
            ),
            onPointClick = {},
            scaffoldConfig = ChartScaffoldConfig(
                axisColor = MaterialTheme.colorScheme.onSurfaceVariant,
                labelTextStyle = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                showGrid = false
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
                    imageVector = Icons.Filled.Balance,
                    contentDescription = "Average",
                )

                Text(
                    text = "Average monthly spend: ${formatCurrency(averageAmount)}",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            if (percentageChange != 0.0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (percentageChange < 0) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                            contentDescription = "Trending Down",
                            tint = MaterialTheme.colorScheme.surfaceBright
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = "Trending Up",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }

                    Text(
                        text = buildAnnotatedString {
                            append("Spending ")

                            withStyle(
                                style = SpanStyle(
                                    color = (
                                        if (percentageChange < 0) MaterialTheme.colorScheme.surfaceBright
                                        else MaterialTheme.colorScheme.error
                                        )
                                )
                            ) {
                                append("${if (percentageChange < 0) "decreased" else "increased"} by ${percentageChange.absoluteValue.roundToInt()}%")
                            }

                            append(" from last month")
                        },
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            if (biggestSpike != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = "Warning",
                        tint = MaterialTheme.colorScheme.error
                    )

                    Text(
                        text = "Unusual spike detected in ${toFullMonthName(biggestSpike.monthName)}",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}
