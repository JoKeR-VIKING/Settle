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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.crashlytics.FirebaseCrashlytics
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

    // Pair expenses with their corresponding sms IDs to maintain correct mapping after grouping
    val itemsWithSmsId = remember(expenses, smsExpenseIds) {
        expenses.mapIndexed { index, expense ->
            expense to smsExpenseIds?.getOrNull(index)
        }
    }

    // Attach minimal debug data to Crashlytics to help reproduce list crashes
    SideEffect {
        val crashlytics = FirebaseCrashlytics.getInstance()
        val summary = itemsWithSmsId.joinToString { (exp, smsId) ->
            "{id:${exp.id}, sms:${smsId}, t:${exp.timestamp}}"
        }
        crashlytics.setCustomKey("expense_table_size", itemsWithSmsId.size)
        crashlytics.log("Table State: $summary")
    }

    val groupedExpenses = remember(itemsWithSmsId) {
        itemsWithSmsId.groupBy { (expense, _) ->
            formatter.format(Date(expense.timestamp))
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        groupedExpenses.forEach { (monthYear, monthItems) ->
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
                monthItems,
                key = { index, (expense, smsId) ->
                    // Priority: unique expense id -> sms draft id -> fallback with timestamp and index
                    when {
                        expense.id.isNotEmpty() -> expense.id
                        !smsId.isNullOrEmpty() -> smsId
                        else -> "${expense.timestamp}_${monthYear}_$index"
                    }
                }
            ) { _, (expense, smsId) ->
                ExpenseRow(
                    expense = expense,
                    smsExpenseId = smsId,
                    toggleSmsModal = toggleSmsModal,
                    onEditExpense = onEditExpense,
                    onDeleteExpense = onDeleteExpense
                )
            }
        }
    }
}
