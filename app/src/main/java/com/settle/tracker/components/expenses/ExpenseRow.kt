package com.settle.tracker.components.expenses

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.settle.tracker.AppDatabase
import com.settle.tracker.components.ConfirmAlertDialog
import com.settle.tracker.scheme.ExpenseCategory
import com.settle.tracker.scheme.ExpenseScheme
import com.settle.tracker.utils.formatCurrency
import com.settle.tracker.utils.formatTimestamp
import com.settle.tracker.utils.getExpenseCategoryColor
import com.settle.tracker.utils.getExpenseCategoryIcon
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

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

    var balanceAmount by remember { mutableStateOf(0.0) }

    fun getExpenseSubText(): String {
        return if (expense.paidBy.isEmpty()) {
            "paid via ${expense.paidFrom}"
        } else if (expense.paidBy.size == 1) {
            "${expense.paidBy.first().name} paid ${formatCurrency(expense.amount)}"
        } else {
            "multiple people paid ${formatCurrency(expense.amount)}"
        }
    }

    LaunchedEffect(expense.splits) {
        if (expense.splits.isEmpty()) return@LaunchedEffect

        val splits = expense.splits
        val payers = expense.paidBy
        val currentUser = Firebase.auth.currentUser

        val mySplit = splits.firstOrNull { it.id == currentUser?.uid }?.amount ?: 0.0
        val myPaid = payers.firstOrNull { it.id == currentUser?.uid }?.amount ?: 0.0

        balanceAmount = myPaid - mySplit
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (expense.category == ExpenseCategory.SETTLEMENT.name) return@combinedClickable
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
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                modifier = Modifier.weight(1f),
                                text = getExpenseSubText(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 0.3.sp,
                                maxLines = 1
                            )

                            Text(
                                text = when {
                                    expense.paidBy.isNotEmpty() -> formatCurrency(balanceAmount.absoluteValue)
                                    else -> formatCurrency(expense.amount)
                                },
                                style = MaterialTheme.typography.labelLarge,
                                letterSpacing = 0.3.sp,
                                color = when {
                                    expense.paidBy.isNotEmpty() -> {
                                        if (balanceAmount >= 0.0) MaterialTheme.colorScheme.surfaceBright
                                        else MaterialTheme.colorScheme.error
                                    }
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        ConfirmAlertDialog(
            title = "Delete Expense",
            text = "Are you sure you want to delete this expense?",
            subText = "This action cannot be undone.",
            onConfirm = {
                if (smsExpenseId != null) {
                    scope.launch {
                        expenseDao.delete(smsExpenseId)
                    }
                } else {
                    onDeleteExpense(expense.id)
                }

                showDeleteDialog = false
            },
            toggleAlert = { showDeleteDialog = false }
        )
    }
}
