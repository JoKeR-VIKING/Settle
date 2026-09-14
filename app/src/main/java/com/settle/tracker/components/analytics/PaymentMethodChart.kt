package com.settle.tracker.components.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.settle.tracker.components.chart.PieChart
import com.settle.tracker.components.chart.PieData
import com.settle.tracker.scheme.ExpenseScheme
import com.settle.tracker.utils.formatCurrency
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

private val SOURCE_COLORS = listOf(
    Color(0xFF6366F1),
    Color(0xFFF59E0B),
    Color(0xFF10B981),
    Color(0xFF3B82F6),
    Color(0xFFEC4899),
    Color(0xFF6B7280),
)

enum class PaymentMethodFilter(val monthCount: Int, val displayName: String) {
    ONE(1, "This month"),
    THREE(3, "Last 3 months"),
    SIX(6, "Last 6 months")
}

@Composable
fun PaymentMethodChart(expenses: List<ExpenseScheme>) {
    if (expenses.isEmpty()) return

    var selectedFilter by remember { mutableStateOf(PaymentMethodFilter.ONE) }

    val pieData = remember(expenses, selectedFilter) {
        val currentMonth = YearMonth.now()
        val startMonth = currentMonth.minusMonths((selectedFilter.monthCount - 1).toLong())

        val filtered = expenses.filter { expense ->
            val expenseMonth = Instant.ofEpochMilli(expense.timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .let { d -> YearMonth.of(d.year, d.month) }
            expenseMonth in startMonth..currentMonth && expense.paidFrom.isNotBlank()
        }

        val grouped = filtered
            .groupBy { it.paidFrom.trim() }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
            .entries
            .sortedByDescending { it.value }

        if (grouped.isEmpty()) return@remember emptyList<PieData>()

        val top5 = grouped.take(5)
        val otherSum = grouped.drop(5).sumOf { it.value }

        val result = top5.mapIndexed { index, (label, amount) ->
            PieData(label = label, value = amount.toFloat(), color = SOURCE_COLORS[index])
        }.toMutableList()

        if (otherSum > 0) {
            result.add(PieData(label = "Other", value = otherSum.toFloat(), color = SOURCE_COLORS.last()))
        }

        result.toList()
    }

    if (pieData.isEmpty()) return

    val topSource = pieData.maxByOrNull { it.value }
    val total = pieData.sumOf { it.value.toDouble() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(PaymentMethodFilter.entries, key = { it.name }) { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter.displayName) }
                )
            }
        }

        PieChart(
            modifier = Modifier.size(220.dp),
            data = pieData,
            donutMode = true,
            showPercentage = true
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            pieData.forEach { item ->
                val pct = if (total > 0) (item.value / total * 100).toInt() else 0
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(item.color)
                    )
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.weight(1f),
                        maxLines = 1
                    )
                    Text(
                        text = "${formatCurrency(item.value.toDouble())}  ($pct%)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (topSource != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.AccountBalanceWallet,
                    contentDescription = null,
                    tint = SOURCE_COLORS[0]
                )
                Text(
                    text = "You spend the most via ${topSource.label}",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
