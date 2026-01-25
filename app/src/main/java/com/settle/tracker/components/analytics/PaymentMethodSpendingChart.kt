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
import androidx.compose.ui.unit.dp
import com.himanshoe.charty.bar.HorizontalBarChart
import com.himanshoe.charty.bar.config.BarChartConfig
import com.himanshoe.charty.bar.data.BarData
import com.himanshoe.charty.color.ChartyColor
import com.himanshoe.charty.common.config.Animation
import com.himanshoe.charty.common.config.ChartScaffoldConfig
import com.himanshoe.charty.common.config.CornerRadius
import com.himanshoe.charty.common.tooltip.TooltipConfig
import com.settle.tracker.scheme.ExpenseScheme
import com.settle.tracker.utils.formatCurrency

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

    fun getPaymentSpendingChart() {
        paymentMethodInfo = expenses
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
        HorizontalBarChart(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(200.dp),
            data = {
                paymentMethodInfo.map { info ->
                    BarData(
                        label = info.paidFrom,
                        value = info.amount.toFloat()
                    )
                }
            },
            color = ChartyColor.Solid(MaterialTheme.colorScheme.primary),
            barConfig = BarChartConfig(
                barWidthFraction = 0.6f,
                cornerRadius = CornerRadius.ExtraLarge,
                animation = Animation.Enabled(),
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
                ),
                showGrid = false
            )
        )
    }
}
