package com.settle.tracker.components.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.settle.tracker.scheme.ExpenseScheme
import com.settle.tracker.utils.formatCurrency

/**
 * A well-known merchant plus the keywords that identify it in an expense's
 * description (matched case-insensitively as a substring). Order matters only
 * for display; matching checks every entry.
 */
data class Vendor(
    val name: String,
    val keywords: List<String>,
    val color: Color
)

val POPULAR_VENDORS = listOf(
    Vendor("Zomato", listOf("zomato"), Color(0xFFE23744)),
    Vendor("Swiggy", listOf("swiggy", "instamart"), Color(0xFFFC8019)),
    Vendor("Amazon", listOf("amazon", "amzn"), Color(0xFFFF9900)),
    Vendor("Flipkart", listOf("flipkart"), Color(0xFF2874F0)),
    Vendor("Blinkit", listOf("blinkit", "grofers"), Color(0xFFF8CB46)),
    Vendor("Zepto", listOf("zepto"), Color(0xFF7B2FF7)),
    Vendor("Uber", listOf("uber"), Color(0xFF000000)),
    Vendor("Ola", listOf("ola cabs", "olacabs", "ola "), Color(0xFF3C9E42)),
    Vendor("BigBasket", listOf("bigbasket", "big basket"), Color(0xFF84C225)),
    Vendor("Myntra", listOf("myntra"), Color(0xFFFF3E6C)),
    Vendor("Rapido", listOf("rapido"), Color(0xFFFFCC00)),
    Vendor("Dominos", listOf("domino", "dominos"), Color(0xFF006491)),
)

data class VendorSpend(
    val name: String,
    val total: Double,
    val count: Int,
    val color: Color
)

fun detectVendor(details: String): Vendor? {
    val lower = details.lowercase()
    return POPULAR_VENDORS.firstOrNull { vendor ->
        vendor.keywords.any { lower.contains(it) }
    }
}

@Composable
fun VendorSpendChart(expenses: List<ExpenseScheme>) {
    val vendorSpends = remember(expenses) {
        val map = mutableMapOf<String, VendorSpend>()
        expenses.forEach { expense ->
            val vendor = detectVendor(expense.details) ?: return@forEach
            val current = map[vendor.name]
            map[vendor.name] = VendorSpend(
                name = vendor.name,
                total = (current?.total ?: 0.0) + expense.amount,
                count = (current?.count ?: 0) + 1,
                color = vendor.color
            )
        }
        map.values.sortedByDescending { it.total }
    }

    if (vendorSpends.isEmpty()) {
        Text(
            text = "No spends at popular vendors yet.\nExpenses mentioning Zomato, Swiggy, Amazon, Flipkart & more will show up here.",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
        )
        return
    }

    val maxSpend = vendorSpends.first().total

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        vendorSpends.forEach { vendor ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(vendor.color.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = vendor.name.first().toString(),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                        fontWeight = FontWeight.Bold,
                        color = vendor.color
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
                            text = "${vendor.name}  ·  ${vendor.count}x",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                        Text(
                            text = formatCurrency(vendor.total),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    LinearProgressIndicator(
                        progress = { if (maxSpend > 0) (vendor.total / maxSpend).toFloat() else 0f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(50)),
                        color = vendor.color,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = StrokeCap.Round
                    )
                }
            }
        }
    }
}
