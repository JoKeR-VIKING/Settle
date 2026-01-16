package com.settle.tracker.screens

import android.annotation.SuppressLint
import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.collections.groupBy
import kotlin.collections.emptyList
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.defaultMinSize

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Icon
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.MaterialTheme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Sms

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight

import com.settle.tracker.utils.formatTimestamp
import com.settle.tracker.utils.formatCurrency
import com.settle.tracker.utils.getExpenseCategoryIcon
import com.settle.tracker.utils.getExpenseCategoryColor
import com.settle.tracker.scheme.ExpenseScheme
import com.settle.tracker.scheme.ExpenseDraft

import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.settle.tracker.AppDatabase

@Composable
fun ExpensesScreen(
    onAddExpense: () -> Unit,
    onEditExpense: (ExpenseDraft) -> Unit,
    currentUser: FirebaseUser,
    openSmsModal: Boolean
) {
    val focusManager = LocalFocusManager.current

    val db = Firebase.firestore

    var searchQuery by remember { mutableStateOf("") }
    var debouncedQuery by remember { mutableStateOf("") }
    var expenses by remember { mutableStateOf<List<ExpenseScheme>>(emptyList()) }
    var expanded by remember { mutableStateOf(false) }

    val filteredExpenses = remember(debouncedQuery, expenses) {
        if (debouncedQuery.isBlank()) {
            expenses
        } else {
            expenses.filter { expense ->
                val words = expense.details.lowercase().split("\\s+".toRegex())
                words.any { word -> word.startsWith(searchQuery) }
            }
        }
    }

    val onDeleteExpense: (String) -> Unit = { expenseId ->
        db
            .collection("users")
            .document(currentUser.uid)
            .collection("expenses")
            .document(expenseId)
            .delete()
            .addOnFailureListener {
                Log.e("Firestore", "${it.message}")
            }
    }

    LaunchedEffect(Unit) {
        db
            .collection("users")
            .document(currentUser.uid)
            .collection("expenses")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapShot, error ->
                if (error != null) {
                    Log.e("Firestore", "${error.message}")
                    return@addSnapshotListener
                }

                if (snapShot != null) {
                    expenses = snapShot.documents.mapNotNull { doc ->
                        ExpenseScheme(
                            id = doc.getString("id") ?: "",
                            timestamp = doc.getLong("timestamp") ?: 0L,
                            details = doc.getString("details") ?: "",
                            amount = doc.getDouble("amount") ?: 0.0,
                            category = doc.getString("category") ?: "",
                            paidFrom = doc.getString("paidFrom") ?: "",
                            createdAt = doc.getLong("createdAt") ?: 0L
                        )
                    }
                }
            }
    }

    LaunchedEffect(searchQuery) {
        delay(300)
        debouncedQuery = searchQuery
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize(),
        floatingActionButton = {
            FabMenu(
                expanded = expanded,
                onToggleExpanded = {
                    expanded = !expanded
                },
                onAddExpense = onAddExpense,
                onEditExpense = onEditExpense,
                openSmsModal = openSmsModal
            )
        },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    focusManager.clearFocus()
                },
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(0.85f),
                    shape = RoundedCornerShape(50),
                    textStyle = MaterialTheme.typography.labelLarge,
                    placeholder = { Text("Search", style = MaterialTheme.typography.labelLarge) },
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search",
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Clear,
                                    contentDescription = "Clear Search",
                                )
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                ExpenseTable(
                    expenses = filteredExpenses,
                    onEditExpense = onEditExpense,
                    onDeleteExpense = onDeleteExpense,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (expanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .pointerInput(Unit) {
                        detectTapGestures {
                            expanded = false
                        }
                    }
            )
        }
    }
}

@Composable
fun ExpenseTable(
    expenses: List<ExpenseScheme>,
    smsExpenseIds: List<String>? = null,
    onEditExpense: (ExpenseDraft) -> Unit,
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
                    onEditExpense = onEditExpense,
                    onDeleteExpense = onDeleteExpense
                )
                HorizontalDivider(color = Color.Gray.copy(0.4f))
            }
        }
    }
}

@Composable
fun ExpenseRow(
    expense: ExpenseScheme,
    smsExpenseId: String? = null,
    onEditExpense: (ExpenseDraft) -> Unit,
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
                    val expenseDraft = ExpenseDraft(
                        id = expense.id,
                        amount = expense.amount,
                        details = expense.details,
                        category = expense.category,
                        paidFrom = expense.paidFrom,
                        timestamp = expense.timestamp,
                        smsExpenseId = smsExpenseId
                    )
                    onEditExpense(expenseDraft)
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

@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FabMenu(
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onAddExpense: () -> Unit,
    onEditExpense: (ExpenseDraft) -> Unit,
    openSmsModal: Boolean
) {
    var showSmsModal by remember { mutableStateOf(openSmsModal) }

    val smsModalSheetstate = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )
    val context = LocalContext.current
    val expenseDao = AppDatabase
        .getInstance(context)
        .expenseDraftDao()
    val drafts by expenseDao
        .getAll()
        .collectAsState(initial = emptyList())
    val expenses = drafts.map {
        ExpenseScheme(
            id = "",
            timestamp = it.timestamp,
            details = it.details,
            amount = it.amount,
            category = it.category,
            paidFrom = it.paidFrom,
            createdAt = it.createdAt
        )
    }
    val expenseIds = drafts.map { it.id }
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val partialHeight = screenHeight * 0.45f

    val rotation by animateFloatAsState(
        targetValue = if (expanded) -45f else 0f,
        animationSpec = tween(durationMillis = 250),
        label = "Fab Rotation"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomEnd
    ) {
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            AnimatedVisibility(
                visible = expanded,
                enter = slideInHorizontally(
                    initialOffsetX = { it }
                ) + expandHorizontally(
                    expandFrom = Alignment.End
                ) + fadeIn(),
                exit = slideOutHorizontally(
                    targetOffsetX = { it }
                ) + shrinkHorizontally(
                    shrinkTowards = Alignment.End
                ) + fadeOut()
            ) {
                SmallFab(
                    label = "Add Expense",
                    Icons.Filled.Receipt,
                    onClick = onAddExpense
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = slideInHorizontally(
                    initialOffsetX = { it }
                ) + expandHorizontally(
                    expandFrom = Alignment.End
                ),
                exit = slideOutHorizontally(
                    targetOffsetX = { it }
                ) + shrinkHorizontally(
                    shrinkTowards = Alignment.End
                )
            ) {
                SmallFab(
                    "Add From SMS",
                    Icons.Filled.Sms,
                    onClick = {
                        showSmsModal = true
                        onToggleExpanded()
                    }
                )
            }

            FloatingActionButton(
                onClick = onToggleExpanded,
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Expense Floating Button",
                    modifier = Modifier.rotate(rotation)
                )
            }
        }

        if (showSmsModal) {
            ModalBottomSheet(
                sheetState = smsModalSheetstate,
                onDismissRequest = { showSmsModal = false }
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 16.dp)
                    ) {
                        Text(
                            "${expenses.size}/50",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.align(Alignment.CenterEnd),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (expenses.isNotEmpty()) {
                        ExpenseTable(
                            expenses = expenses,
                            smsExpenseIds = expenseIds,
                            onEditExpense = onEditExpense,
                            onDeleteExpense = {},
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = partialHeight)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = partialHeight),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Sms,
                                    contentDescription = "Empty SMS"
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    "No SMS expenses found",
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SmallFab(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    SmallFloatingActionButton(
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier.size(18.dp),
                imageVector = icon,
                contentDescription = null
            )

            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}
