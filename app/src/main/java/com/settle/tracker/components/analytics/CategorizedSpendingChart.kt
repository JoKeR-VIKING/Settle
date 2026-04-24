package com.settle.tracker.components.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.settle.tracker.components.analytics.personal.getHighestCategorySpend
import com.settle.tracker.components.analytics.personal.prepareCategorizedData
import com.settle.tracker.components.chart.PieChart
import com.settle.tracker.scheme.ExpenseScheme
import com.settle.tracker.utils.formatCurrency
import com.settle.tracker.utils.getExpenseCategoryColor
import kotlin.math.roundToInt

enum class CategoryFilterList(
    val monthCount: Int,
    val displayName: String
) {
    ONE(1, "Current month"),
    THREE(3, "Last 3 months");
}

@Composable
fun CategorizedSpendingChart(
    expenses: List<ExpenseScheme>
) {
    if (expenses.size <= 1) return

    var selectedFilter by remember { mutableStateOf(CategoryFilterList.ONE) }
    val categorizedData = remember(expenses, selectedFilter) {
        prepareCategorizedData(
            expenses,
            selectedFilter.monthCount
        )
    }

    val highestCategorySpends = remember(expenses) {
        getHighestCategorySpend(expenses)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(CategoryFilterList.entries, key = { it.name }) { filterValue ->
                FilterChip(
                    selected = selectedFilter == filterValue,
                    onClick = { selectedFilter = filterValue },
                    label = { Text(filterValue.displayName) }
                )
            }
        }

        PieChart(
            modifier = Modifier.size(250.dp),
            data = categorizedData,
            donutMode = true,
            showPercentage = true
        )

        FlowRow(
            maxItemsInEachRow = 2,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            categorizedData.forEach { category ->
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .background(getExpenseCategoryColor(category.label))
                    )

                    Text(
                        text = "${
                            category.label.lowercase().replaceFirstChar { it.uppercase() }
                        } - ${formatCurrency(category.value.toDouble())}",
                        style = MaterialTheme.typography.labelSmall
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
                                append(
                                    data.category.lowercase().replaceFirstChar { it.uppercase() })
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
