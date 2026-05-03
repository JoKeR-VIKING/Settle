package com.settle.tracker.components.expenses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.settle.tracker.scheme.ExpenseScheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ExpenseTable(
    expenses: List<ExpenseScheme>,
    smsExpenseIds: List<String>? = null,
    toggleSmsModal: () -> Unit = {},
    onEditExpense: (String, String?) -> Unit,
    onDeleteExpense: (String) -> Unit,
    modifier: Modifier
) {
    val formatter = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }

    val groupedExpenses = remember(expenses) {
        expenses.groupBy { formatter.format(Date(it.timestamp)) }
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        groupedExpenses.forEach { (monthYear, monthExpenses) ->
            item(key = "header-$monthYear") {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 6.dp),
                    text = monthYear,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.6.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            itemsIndexed(
                monthExpenses,
                key = { _, expense ->
                    "${expense.timestamp}_${expense.id}"
                }
            ) { index, expense ->
                ExpenseRow(
                    expense = expense,
                    smsExpenseId = smsExpenseIds?.getOrNull(index),
                    toggleSmsModal = toggleSmsModal,
                    onEditExpense = onEditExpense,
                    onDeleteExpense = onDeleteExpense
                )
            }
        }
    }
}
