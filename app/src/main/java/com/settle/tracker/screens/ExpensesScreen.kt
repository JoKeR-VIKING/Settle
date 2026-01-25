package com.settle.tracker.screens

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
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
import com.settle.tracker.components.common.FabOverlay
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

    Scaffold(
        modifier = Modifier
            .fillMaxSize(),
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
            SecondaryTabRow(
                selectedTabIndex = selectedTab.ordinal,
            ) {
                Tab(
                    selected = selectedTab == DashboardType.EXPENSES,
                    onClick = {  selectedTab = DashboardType.EXPENSES },
                    text = {
                        Text(
                            text = DashboardType.EXPENSES.getDisplayName(),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                )

                Tab(
                    selected = selectedTab == DashboardType.RECURRING_EXPENSES,
                    onClick = { selectedTab = DashboardType.RECURRING_EXPENSES },
                    text = {
                        Text(
                            text = DashboardType.RECURRING_EXPENSES.getDisplayName(),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                )
            }

            if (selectedTab == DashboardType.EXPENSES) {
                ExpenseTable(
                    expenses = expenses,
                    onEditExpense = onEditExpense,
                    onDeleteExpense = onDeleteExpense,
                    modifier = Modifier
                )
            } else {
                RecurringExpensesList(
                    ownerCollection = "users",
                    ownerId = currentUser.uid,
                )
            }
        }

        FabOverlay(
            visible = expanded,
            onDismiss = { expanded = false }
        )
    }
}
