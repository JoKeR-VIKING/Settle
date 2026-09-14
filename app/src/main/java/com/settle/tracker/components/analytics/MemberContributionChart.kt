package com.settle.tracker.components.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.settle.tracker.scheme.ExpenseScheme
import com.settle.tracker.utils.formatCurrency

data class MemberContribution(
    val userId: String,
    val name: String,
    val totalPaid: Double,
    val percentage: Float
)

@Composable
fun MemberContributionChart(expenses: List<ExpenseScheme>) {
    if (expenses.isEmpty()) return

    val contributions = remember(expenses) {
        val map = mutableMapOf<String, Pair<String, Double>>()
        expenses.forEach { expense ->
            expense.paidBy.forEach { payer ->
                val current = map[payer.id] ?: (payer.name to 0.0)
                map[payer.id] = current.first to (current.second + payer.amount)
            }
        }
        val total = map.values.sumOf { it.second }
        if (total == 0.0) return@remember emptyList<MemberContribution>()
        map.entries
            .map { (id, pair) ->
                MemberContribution(
                    userId = id,
                    name = pair.first,
                    totalPaid = pair.second,
                    percentage = (pair.second / total * 100).toFloat()
                )
            }
            .sortedByDescending { it.totalPaid }
    }

    if (contributions.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        contributions.forEachIndexed { index, member ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = member.name.ifBlank { "Unknown" },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                        Text(
                            text = "${formatCurrency(member.totalPaid)}  (${member.percentage.toInt()}%)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    LinearProgressIndicator(
                        progress = { member.percentage / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(50)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = StrokeCap.Round
                    )
                }
            }
        }
    }
}
