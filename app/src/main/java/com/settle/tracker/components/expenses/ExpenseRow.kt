package com.settle.tracker.components.expenses

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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

    val currentUser = Firebase.auth.currentUser
    val isInPaidBy = expense.paidBy.any { it.id == currentUser?.uid }
    val isInSplits = expense.splits.any { it.id == currentUser?.uid }
    val isInvolved = isInPaidBy || isInSplits

    var balanceAmount by remember { mutableStateOf(0.0) }

    fun getExpenseSubText(): String {
        return if (expense.paidBy.isEmpty()) {
            "via ${expense.paidFrom}"
        } else if (expense.paidBy.size == 1) {
            "${expense.paidBy.first().name} paid"
        } else {
            "Multiple people paid"
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

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
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
            ),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 2.dp,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 15.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        getExpenseCategoryColor(expense.category).copy(alpha = 0.15f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier.size(24.dp),
                    imageVector = getExpenseCategoryIcon(expense.category),
                    contentDescription = "Category Icon",
                    tint = getExpenseCategoryColor(expense.category)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = expense.details,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = getExpenseSubText(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = " • ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatTimestamp(timestamp = expense.timestamp, format = "MMM dd"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                val amountText = when {
                    expense.paidBy.isNotEmpty() -> {
                        if (balanceAmount.absoluteValue == 0.0 && !isInvolved)
                            "Not involved"
                        else
                            formatCurrency(balanceAmount.absoluteValue)
                    }
                    else -> formatCurrency(expense.amount)
                }

                Text(
                    text = amountText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        expense.paidBy.isNotEmpty() -> {
                            if (balanceAmount > 0.0) MaterialTheme.colorScheme.surfaceBright
                            else if (balanceAmount < 0.0) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurface
                        }
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )

                if (expense.paidBy.isNotEmpty() && (balanceAmount != 0.0 || isInvolved)) {
                    Text(
                        text = if (balanceAmount > 0) "you get back" else if (balanceAmount < 0) "you owe" else "settled",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
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
