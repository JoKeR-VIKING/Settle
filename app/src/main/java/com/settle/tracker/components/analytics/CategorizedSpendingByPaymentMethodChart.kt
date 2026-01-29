package com.settle.tracker.components.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.unit.dp
import com.settle.tracker.components.chart.PieChart
import com.settle.tracker.components.chart.PieData
import com.settle.tracker.scheme.ExpenseScheme
import com.settle.tracker.utils.colorFromString
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

@Composable
fun CategorizedSpendingByPaymentMethodChart(
    expenses: List<ExpenseScheme>
) {
    if (expenses.size <= 1) return

    var categorizedData by remember { mutableStateOf(emptyList<PieData>()) }

    fun getCategorizedPaymentMethod() {
        val currentMonth = YearMonth.now()

        categorizedData = expenses
            .filter {
                val expenseMonth = Instant.ofEpochMilli(it.timestamp)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                    .let { date -> YearMonth.of(date.year, date.month) }

                expenseMonth == currentMonth
            }
            .groupBy { it.paidFrom }
            .map { (paidFrom, paymentExpenses) ->
                PieData(
                    label = paidFrom,
                    value = paymentExpenses.sumOf { it.amount }.toFloat(),
                    color = colorFromString(paidFrom)
                )
            }
    }

    LaunchedEffect(expenses) {
        getCategorizedPaymentMethod()
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
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
                ) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .background(colorFromString(category.label))
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = category.label.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}
