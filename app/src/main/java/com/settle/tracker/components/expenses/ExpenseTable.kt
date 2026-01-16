package com.settle.tracker.components.expenses

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.settle.tracker.AppDatabase
import com.settle.tracker.scheme.ExpenseScheme
import com.settle.tracker.utils.formatCurrency
import com.settle.tracker.utils.formatTimestamp
import com.settle.tracker.utils.getExpenseCategoryColor
import com.settle.tracker.utils.getExpenseCategoryIcon
import kotlinx.coroutines.launch
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExpenseRow(
    expense: ExpenseScheme,
    smsExpenseId: String? = null,
    toggleSmsModal: () -> Unit,
    onEditExpense: (String, String?) -> Unit,
    onDeleteExpense: (String) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val expenseDao = AppDatabase
        .getInstance(context)
        .expenseDraftDao()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (expense.id.isBlank()) toggleSmsModal()

                    onEditExpense(
                        if (expense.id.isNotBlank()) "EDIT" else "SMS_ADD",
                        expense.id.ifBlank { smsExpenseId }
                    )
                },
                onLongClick = {
                    showDeleteDialog = true
                }
            )
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                formatTimestamp(timestamp = expense.timestamp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.3.sp,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                getExpenseCategoryColor(expense.category),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            modifier = Modifier.size(25.dp),
                            imageVector = getExpenseCategoryIcon(expense.category),
                            contentDescription = "Expense Icon",
                            tint = MaterialTheme.colorScheme.onSecondary
                        )
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            expense.details,
                            style = MaterialTheme.typography.labelLarge,
                            letterSpacing = 0.5.sp,
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "paid via ${expense.paidFrom}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 0.3.sp,
                            )

                            Text(
                                formatCurrency(expense.amount),
                                style = MaterialTheme.typography.labelLarge,
                                letterSpacing = 0.3.sp,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    "Delete Expense",
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Are you sure you want to delete this expense?",
                        style = MaterialTheme.typography.labelMedium,
                        letterSpacing = 0.5.sp
                    )

                    Text(
                        "This action cannot be undone.",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (smsExpenseId != null) {
                            scope.launch {
                                expenseDao.delete(smsExpenseId)
                            }
                        } else {
                            onDeleteExpense(expense.id)
                        }

                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    )
                ) {
                    Text(
                        "Delete",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ),
                    border = BorderStroke(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        "Cancel",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        )
    }
}
