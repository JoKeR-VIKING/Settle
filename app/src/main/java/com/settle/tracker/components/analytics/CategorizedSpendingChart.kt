package com.settle.tracker.components.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
import com.himanshoe.charty.pie.PieChart
import com.himanshoe.charty.pie.config.LabelConfig
import com.himanshoe.charty.pie.config.PieChartConfig
import com.himanshoe.charty.pie.config.PieChartStyle
import com.settle.tracker.components.analytics.personal.getHighestCategorySpend
import com.settle.tracker.components.analytics.personal.prepareCategorizedData
import com.settle.tracker.scheme.ExpenseScheme
import com.settle.tracker.utils.getExpenseCategoryColor
import kotlin.math.roundToInt

@Composable
fun CategorizedSpendingChart(
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
