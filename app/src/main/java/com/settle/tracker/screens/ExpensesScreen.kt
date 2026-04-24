package com.settle.tracker.screens

import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.settle.tracker.components.FabMenu
import com.settle.tracker.components.common.ExpenseListSkeleton
import com.settle.tracker.components.common.EmptyState
import com.settle.tracker.components.common.FabOverlay
import com.settle.tracker.components.common.ScreenHeader
import com.settle.tracker.components.expenses.ExpenseTable
import com.settle.tracker.components.expenses.RecurringExpensesList
import com.settle.tracker.scheme.ExpenseScheme

enum class DashboardType {
    EXPENSES,
    RECURRING_EXPENSES;

    fun getDisplayName(): String =
        this.name
            .lowercase()
            .split('_', ' ')
            .joinToString(" ") { word ->
                word.replaceFirstChar { it.uppercase() }
            }
}

@Composable
fun ExpensesScreen(
    onAddExpense: () -> Unit,
    onEditExpense: (String, String?) -> Unit,
    currentUser: FirebaseUser
) {
    val focusManager = LocalFocusManager.current

    val db = Firebase.firestore

    var expenses by remember { mutableStateOf<List<ExpenseScheme>>(emptyList()) }
    var isFetchingExpenses by remember { mutableStateOf(true) }
    var expanded by remember { mutableStateOf(false) }

    var selectedTab by remember { mutableStateOf(DashboardType.EXPENSES) }

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
                    isFetchingExpenses = false
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
                isFetchingExpenses = false
            }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        floatingActionButton = {
            FabMenu(
                expanded = expanded,
                onToggleExpanded = {
                    expanded = !expanded
                },
                onAddExpense = onAddExpense,
                onEditExpense = onEditExpense
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    focusManager.clearFocus()
                },
        ) {
            ScreenHeader(
                title = "Personal expenses",
                subtitle = "Track everyday spending, recurring payments, and quick adds from one place."
            )

            SecondaryTabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.background,
                indicator = {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(selectedTab.ordinal),
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                divider = {}
            ) {
                DashboardType.entries.forEach { type ->
                    Tab(
                        selected = selectedTab == type,
                        onClick = { selectedTab = type },
                        text = {
                            Text(
                                text = type.getDisplayName(),
                                style = if (selectedTab == type)
                                    MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                else
                                    MaterialTheme.typography.titleSmall,
                                color = if (selectedTab == type)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }

            AnimatedContent(
                targetState = selectedTab,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    fadeIn(animationSpec = tween(180))
                        .togetherWith(fadeOut(animationSpec = tween(140)))
                },
                label = "expenses-tab-content"
            ) { activeTab ->
                Box(modifier = Modifier.fillMaxSize()) {
                    if (activeTab == DashboardType.EXPENSES) {
                        when {
                            isFetchingExpenses && expenses.isEmpty() -> {
                                ExpenseListSkeleton(modifier = Modifier.fillMaxSize())
                            }

                            expenses.isEmpty() -> {
                                EmptyState(
                                    assetName = "empty_expenses.json",
                                    title = "No expenses yet",
                                    description = "Add your first expense to start building a cleaner picture of your spending."
                                )
                            }

                            else -> {
                                ExpenseTable(
                                    expenses = expenses,
                                    onEditExpense = onEditExpense,
                                    onDeleteExpense = onDeleteExpense,
                                    modifier = Modifier
                                )
                            }
                        }
                    } else {
                        RecurringExpensesList(
                            ownerCollection = "users",
                            ownerId = currentUser.uid,
                        )
                    }
                }
            }
        }

        FabOverlay(
            visible = expanded,
            onDismiss = { expanded = false }
        )
    }
}
