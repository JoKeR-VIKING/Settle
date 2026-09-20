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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallReceived
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.settle.tracker.AppDatabase
import com.settle.tracker.components.ConfirmAlertDialog
import com.settle.tracker.scheme.ExpenseCategory
import com.settle.tracker.scheme.ExpenseScheme
import com.settle.tracker.scheme.ExpenseSource
import com.settle.tracker.ui.theme.Success
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
    val expenseDao = remember { AppDatabase.getInstance(context).expenseDraftDao() }

    val currentUser = Firebase.auth.currentUser
    val isInPaidBy = expense.paidBy.any { it.id == currentUser?.uid }
    val isInSplits = expense.splits.any { it.id == currentUser?.uid }
    val isInvolved = isInPaidBy || isInSplits

    var balanceAmount by remember(expense.id, expense.splits) { mutableStateOf(0.0) }
    LaunchedEffect(expense.id, expense.splits, expense.paidBy) {
        if (expense.splits.isEmpty()) {
            balanceAmount = 0.0
            return@LaunchedEffect
        }
        val mySplit = expense.splits.firstOrNull { it.id == currentUser?.uid }?.amount ?: 0.0
        val myPaid = expense.paidBy.firstOrNull { it.id == currentUser?.uid }?.amount ?: 0.0
        balanceAmount = myPaid - mySplit
    }

    val categoryColor = getExpenseCategoryColor(expense.category)
    val isSettlement = expense.category == ExpenseCategory.SETTLEMENT.name

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .combinedClickable(
                onClick = {
                    if (isSettlement) return@combinedClickable
                    if (expense.id.isBlank()) toggleSmsModal()
                    onEditExpense(
                        if (expense.id.isNotBlank()) "EDIT" else "SMS_ADD",
                        expense.id.ifBlank { smsExpenseId }
                    )
                },
                onLongClick = { showDeleteDialog = true }
            )
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Category icon — solid tinted background
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(categoryColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = getExpenseCategoryIcon(expense.category),
                contentDescription = null,
                tint = categoryColor,
                modifier = Modifier.size(22.dp)
            )
        }

        // Title + meta row
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (expense.source == ExpenseSource.SMS_AUTO.name) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = "Auto-added",
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = expense.details.ifBlank { "Untitled expense" },
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = formatTimestamp(expense.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!isSettlement) {
                    val subText = expenseSubText(expense)
                    if (subText.isNotBlank()) {
                        Separator()
                        Text(
                            text = subText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.width(2.dp))

        // Amount + lent/owe pill
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val isSplitExpense = expense.paidBy.isNotEmpty()
            val showNotInvolved = isSplitExpense && balanceAmount.absoluteValue == 0.0 && !isInvolved

            Text(
                text = when {
                    showNotInvolved -> "—"
                    isSplitExpense  -> formatCurrency(balanceAmount.absoluteValue)
                    else            -> formatCurrency(expense.amount)
                },
                style = if (showNotInvolved) MaterialTheme.typography.labelMedium
                        else MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                color = when {
                    !isSplitExpense  -> MaterialTheme.colorScheme.onSurface
                    balanceAmount > 0.0 -> Success
                    balanceAmount < 0.0 -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            if (isSplitExpense && balanceAmount != 0.0) {
                val lent = balanceAmount > 0.0
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (lent) Success.copy(alpha = 0.12f)
                            else MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = if (lent) Icons.Filled.CallReceived else Icons.Filled.CallMade,
                        contentDescription = null,
                        modifier = Modifier.size(9.dp),
                        tint = if (lent) Success else MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = if (lent) "lent" else "owe",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = if (lent) Success else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else if (isSettlement) {
                Text(
                    "settled",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showDeleteDialog) {
        ConfirmAlertDialog(
            title = "Delete Expense",
            text = "Are you sure you want to delete this expense?",
            subText = "This action cannot be undone.",
            onConfirm = {
                if (smsExpenseId != null) scope.launch { expenseDao.delete(smsExpenseId) }
                else onDeleteExpense(expense.id)
                showDeleteDialog = false
            },
            toggleAlert = { showDeleteDialog = false }
        )
    }
}

@Composable
private fun Separator() {
    Box(
        Modifier
            .size(3.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
    )
}

private fun expenseSubText(expense: ExpenseScheme): String = when {
    expense.paidBy.isEmpty() -> expense.paidFrom.ifBlank { "Cash" }
    expense.paidBy.size == 1 -> "${expense.paidBy.first().name} paid ${formatCurrency(expense.amount)}"
    else -> "multiple people paid ${formatCurrency(expense.amount)}"
}
