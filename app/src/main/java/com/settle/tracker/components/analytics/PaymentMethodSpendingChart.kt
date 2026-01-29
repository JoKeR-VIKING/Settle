package com.settle.tracker.components.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.SwapHoriz
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.settle.tracker.components.chart.BarChart
import com.settle.tracker.components.chart.BarData
import com.settle.tracker.scheme.ExpenseScheme
import com.settle.tracker.utils.formatCurrency
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

data class PaymentMethodInfo(
    val paidFrom: String,
    val amount: Double
)

@Composable
fun PaymentMethodSpendingChart(
    expenses: List<ExpenseScheme>
) {
    if (expenses.size <= 1) return

    var paymentMethodInfo by remember { mutableStateOf(emptyList<PaymentMethodInfo>()) }

    val mostSpentPaymentMethod = paymentMethodInfo
        .groupingBy { it.paidFrom }
        .fold(0.0) { acc, item -> acc + item.amount }
        .maxByOrNull { it.value }
        ?.key ?: ""
    val mostUsedPaymentMethod = paymentMethodInfo
        .groupingBy { it.paidFrom }
        .eachCount()
        .maxByOrNull { it.value }
        ?.key ?: ""

    fun getPaymentSpendingChart() {
        val currentMonth = YearMonth.now()

        paymentMethodInfo = expenses
            .filter {
                val expenseMonth = Instant.ofEpochMilli(it.timestamp)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                    .let { date -> YearMonth.of(date.year, date.month) }

                expenseMonth == currentMonth
            }
            .groupBy { it.paidFrom }
            .map { (paidFrom, paymentExpenses) ->
                PaymentMethodInfo(
                    paidFrom = paidFrom,
                    amount = paymentExpenses.sumOf { it.amount }
                )
            }
    }

    LaunchedEffect(expenses) {
        getPaymentSpendingChart()
    }

    if (paymentMethodInfo.size <= 1) return

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        BarChart(
            modifier = Modifier.height(250.dp),
            xAxisData = paymentMethodInfo.map { it.paidFrom },
            bars = paymentMethodInfo.map {
                BarData(
                    barValues = listOf(it.amount.toFloat()),
                    barColors = listOf(MaterialTheme.colorScheme.primary)
                )
            },
            tooltipTextStyle = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            ),
            tooltipFormatter = { formatCurrency(it.toDouble()) }
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
                    imageVector = Icons.Filled.EmojiEvents,
                    contentDescription = "Most Spent",
                )

                Text(
                    text = "You spend most using $mostSpentPaymentMethod",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.SwapHoriz,
                    contentDescription = "Most Used",
                )

                Text(
                    text = "You do most of your transactions using $mostUsedPaymentMethod",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}
