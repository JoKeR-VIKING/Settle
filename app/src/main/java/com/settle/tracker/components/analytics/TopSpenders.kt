package com.settle.tracker.components.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.settle.tracker.components.chart.BarChart
import com.settle.tracker.components.chart.BarData
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

        BarChart(
            modifier = Modifier.height(250.dp),
            xAxisData = userSpendList.map { it.userName },
            bars = userSpendList.map { userSpend ->
                BarData(
                    barValues = userSpend.categorySpend.values.map { it.toFloat() },
                    barColors = userSpend.categorySpend.entries.map { (category, _) ->
                        getExpenseCategoryColor(category)
                    }
                )
            },
            barSpacing = 150f,
            tooltipTextStyle = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            ),
            tooltipFormatter = { formatCurrency(it.toDouble()) },
            startFromEnd = false
        )
    }
}
