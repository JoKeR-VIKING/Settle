package com.settle.tracker.components.analytics.personal

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.himanshoe.charty.bar.BarChart
import com.himanshoe.charty.bar.config.BarChartConfig
import com.himanshoe.charty.bar.data.BarData
import com.himanshoe.charty.color.ChartyColor
import com.himanshoe.charty.common.config.Animation
import com.himanshoe.charty.common.config.ChartScaffoldConfig
import com.himanshoe.charty.common.config.CornerRadius
import com.himanshoe.charty.common.config.ReferenceLineConfig
import com.himanshoe.charty.common.tooltip.TooltipConfig
import com.himanshoe.charty.line.LineChart
import com.himanshoe.charty.line.config.LineChartConfig
import com.himanshoe.charty.line.data.LineData
import com.himanshoe.charty.pie.PieChart
import com.himanshoe.charty.pie.config.LabelConfig
import com.himanshoe.charty.pie.config.PieChartConfig
import com.himanshoe.charty.pie.config.PieChartStyle
import com.settle.tracker.scheme.ExpenseScheme
import com.settle.tracker.utils.formatCurrency
import com.settle.tracker.utils.getExpenseCategoryColor
import com.settle.tracker.utils.toFullMonthName
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

@Composable
fun PersonalAnalytics() {
    val db = Firebase.firestore
    val currentUser = Firebase.auth.currentUser

    var expenses by remember { mutableStateOf(emptyList<ExpenseScheme>()) }

    LaunchedEffect(Unit) {
        if (currentUser == null) return@LaunchedEffect

        db
            .collection("users")
            .document(currentUser.uid)
            .collection("expenses")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("Firestore", "${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot == null) return@addSnapshotListener

                expenses = snapshot.toObjects(ExpenseScheme::class.java)
            }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, top = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(35.dp)
    ) {
        item {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "Monthly Spends",
                style = MaterialTheme.typography.bodyLarge
            )
        }

        item {
            MonthlyLineChart(expenses)
        }

        item {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "Daily Spends",
                style = MaterialTheme.typography.bodyLarge
            )
        }

        item {
            DailySpends(expenses)
        }

        item {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "Categorized Spends",
                style = MaterialTheme.typography.bodyLarge
            )
        }

        item {
            CategorizedSpends(expenses)
        }
    }
}

@Composable
fun CategorizedSpends(
    expenses: List<ExpenseScheme>
) {
    if (expenses.size <= 1) return

    val categorizedData = remember(expenses) { prepareCategorizedData(expenses) }
    val pieColors = remember(categorizedData) {
        categorizedData.map { data ->
            getExpenseCategoryColor(data.label)
        }
    }

    val highestCategorySpends = remember(expenses) {
        getHighestCategorySpend(expenses)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        PieChart(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            color = ChartyColor.Gradient(
                pieColors
            ),
            data = {
                categorizedData
            },
            config = PieChartConfig(
                style = PieChartStyle.DONUT,
                donutHoleRatio = 0.5f,
                labelConfig = LabelConfig(
                    minimumPercentageToShowLabel = 8f,
                    labelTextStyle = MaterialTheme.typography.labelMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            )
        )

        FlowRow(
            maxItemsInEachRow = 2,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            categorizedData.forEach { category ->
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .background(getExpenseCategoryColor(category.label))
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = category.label.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            highestCategorySpends.forEach { data ->
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
                            append("You spent ")

                            withStyle(
                                style = SpanStyle(
                                    color = MaterialTheme.colorScheme.error
                                )
                            ) {
                                append("${data.percentageIncrease.roundToInt()}%")
                            }

                            append(" more on ")

                            withStyle(
                                style = SpanStyle(
                                    color = MaterialTheme.colorScheme.error
                                )
                            ) {
                                append(data.category.lowercase().replaceFirstChar { it.uppercase() })
                            }

                            append(" this month ")
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

@Composable
fun DailySpends(
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

@Composable
fun MonthlyLineChart(
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
