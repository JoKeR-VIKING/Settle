package com.settle.tracker.components.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.himanshoe.charty.bar.StackedBarChart
import com.himanshoe.charty.bar.config.StackedBarChartConfig
import com.himanshoe.charty.bar.data.BarGroup
import com.himanshoe.charty.color.ChartyColor
import com.himanshoe.charty.common.config.Animation
import com.himanshoe.charty.common.config.ChartScaffoldConfig
import com.himanshoe.charty.common.config.CornerRadius
import com.himanshoe.charty.common.tooltip.TooltipConfig
import com.settle.tracker.scheme.ExpenseScheme
import com.settle.tracker.utils.formatCurrency
import com.settle.tracker.utils.getExpenseCategoryColor

data class UserSpend(
    val userId: String,
    val userName: String,
    val categorySpend: Map<String, Double>,
    val total: Double
)

@Composable
fun TopSpenders(
    expenses: List<ExpenseScheme>
) {
    if (expenses.size <= 1) return

    var userSpendList by remember { mutableStateOf(emptyList<UserSpend>()) }

    LaunchedEffect(expenses) {
        val spenderMap = mutableMapOf<String, MutableMap<String, Double>>()

        expenses.forEach { expense ->
            expense.splits.forEach { participant ->
                val userCategoryMap = spenderMap.getOrPut("${participant.id};${participant.name}") {
                    mutableMapOf()
                }
                userCategoryMap[expense.category] =
                    (userCategoryMap[expense.category] ?: 0.0) + participant.amount

                spenderMap["${participant.id};${participant.name}"] = userCategoryMap
            }
        }

        userSpendList = spenderMap
            .map { (userId, categorySpend) ->
                UserSpend(
                    userId = userId.split(";")[0],
                    userName = userId.split(";")[1],
                    categorySpend = categorySpend
                        .entries
                        .sortedBy { it.value }
                        .associateTo(LinkedHashMap()) { it.key to it.value },
                    total = categorySpend.values.sum()
                )
            }
            .sortedByDescending { it.total }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        if (userSpendList.isEmpty()) return

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 30.dp)
        ) {
            item {
                StackedBarChart(
                    modifier = Modifier
                        .width((userSpendList.size * 130).dp)
                        .height(200.dp),
                    data = {
                        userSpendList.map { userSpend ->
                            BarGroup(
                                label = userSpend.userName,
                                values = userSpend.categorySpend.values.map { it.toFloat() },
                                colors = userSpend.categorySpend.entries.map { (category, _) ->
                                    ChartyColor.Solid(
                                        getExpenseCategoryColor(category)
                                    )
                                }
                            )
                        }
                    },
                    stackedConfig = StackedBarChartConfig(
                        barWidthFraction = 0.6f,
                        topCornerRadius = CornerRadius.ExtraLarge,
                        animation = Animation.Enabled(),
                        tooltipConfig = TooltipConfig(
                            backgroundColor = MaterialTheme.colorScheme.secondary
                        ),
                        tooltipFormatter = { lineData ->
                            formatCurrency(lineData.segmentValue.toDouble())
                        }
                    ),
                    onSegmentClick = {},
                    scaffoldConfig = ChartScaffoldConfig(
                        axisColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        labelTextStyle = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                )
            }
        }
    }
}
