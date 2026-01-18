package com.settle.tracker.components.expenses

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.settle.tracker.components.expenses.ExpenseRow
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
    val formatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    fun Long.toMonthYear(): String {
        val date = Date(this)
        return formatter.format(date)
    }

    val groupedExpenses = remember(expenses) {
        expenses.groupBy { it.timestamp.toMonthYear() }
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(1f),
    ) {
        groupedExpenses.forEach { (monthYear, monthExpenses) ->
            item {
                Text(
                    modifier = Modifier.padding(
                        start = 20.dp,
                        top = 14.dp,
                        bottom = 2.dp
                    ),
                    text = monthYear,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            itemsIndexed(
                monthExpenses,
                key = { index, expense ->
                    expense.id.takeIf { it.isNotBlank() }
                        ?: smsExpenseIds?.getOrNull(index)
                        ?: index
                }
            ) { index, expense ->
                ExpenseRow(
                    expense = expense,
                    smsExpenseId = smsExpenseIds?.get(index),
                    toggleSmsModal = toggleSmsModal,
                    onEditExpense = onEditExpense,
                    onDeleteExpense = onDeleteExpense
                )
                HorizontalDivider(color = Color.Gray.copy(0.4f))
            }
        }
    }
}
