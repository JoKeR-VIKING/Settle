package com.settle.tracker.components.expenses

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.animateFloatAsState
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
import com.settle.tracker.utils.rememberSoundManager
import kotlinx.coroutines.delay
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
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(30); visible = true }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val expenseDao = AppDatabase.getInstance(context).expenseDraftDao()
    val sound = rememberSoundManager()

    val currentUser = Firebase.auth.currentUser
    val isInPaidBy = expense.paidBy.any { it.id == currentUser?.uid }
    val isInSplits = expense.splits.any { it.id == currentUser?.uid }
    val isInvolved = isInPaidBy || isInSplits

    var balanceAmount by remember { mutableStateOf(0.0) }

    fun getExpenseSubText(): String = when {
        expense.paidBy.isEmpty() -> "paid via ${expense.paidFrom}"
        expense.paidBy.size == 1 -> "${expense.paidBy.first().name} paid ${formatCurrency(expense.amount)}"
        else -> "multiple people paid ${formatCurrency(expense.amount)}"
    }

    LaunchedEffect(expense.splits) {
        if (expense.splits.isEmpty()) return@LaunchedEffect
        val mySplit = expense.splits.firstOrNull { it.id == currentUser?.uid }?.amount ?: 0.0
        val myPaid = expense.paidBy.firstOrNull { it.id == currentUser?.uid }?.amount ?: 0.0
        balanceAmount = myPaid - mySplit
    }

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.97f else 1f,
        animationSpec = tween(120),
        label = "row-scale"
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(260)) + slideInVertically(
            animationSpec = tween(260),
            initialOffsetY = { it / 4 }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .scale(scale)
                .combinedClickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = {
                        if (expense.category == ExpenseCategory.SETTLEMENT.name) return@combinedClickable
                        sound.tap()
                        if (expense.id.isBlank()) toggleSmsModal()
                        onEditExpense(
                            if (expense.id.isNotBlank()) "EDIT" else "SMS_ADD",
                            expense.id.ifBlank { smsExpenseId }
                        )
                    },
                    onLongClick = {
                        sound.delete()
                        showDeleteDialog = true
                    }
                )
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(getExpenseCategoryColor(expense.category), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier.size(24.dp),
                    imageVector = getExpenseCategoryIcon(expense.category),
                    contentDescription = "Expense Icon",
                    tint = MaterialTheme.colorScheme.onSecondary
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    formatTimestamp(timestamp = expense.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    expense.details.ifBlank { "Untitled" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.2.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = getExpenseSubText(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(6.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = when {
                        expense.paidBy.isNotEmpty() -> {
                            if (balanceAmount.absoluteValue == 0.0 && !isInvolved) "Not involved"
                            else formatCurrency(balanceAmount.absoluteValue)
                        }
                        else -> formatCurrency(expense.amount)
                    },
                    style = when {
                        expense.paidBy.isNotEmpty() && balanceAmount.absoluteValue == 0.0 -> MaterialTheme.typography.labelSmall
                        else -> MaterialTheme.typography.titleMedium
                    },
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.3.sp,
                    maxLines = 1,
                    color = when {
                        expense.paidBy.isNotEmpty() -> {
                            when {
                                balanceAmount > 0.0 -> MaterialTheme.colorScheme.surfaceBright
                                balanceAmount < 0.0 -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        }
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                )
                if (expense.paidBy.isNotEmpty() && balanceAmount != 0.0) {
                    Text(
                        text = if (balanceAmount > 0.0) "you lent" else "you owe",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                if (smsExpenseId != null) scope.launch { expenseDao.delete(smsExpenseId) }
                else onDeleteExpense(expense.id)
                showDeleteDialog = false
            },
            toggleAlert = { showDeleteDialog = false }
        )
    }
}
