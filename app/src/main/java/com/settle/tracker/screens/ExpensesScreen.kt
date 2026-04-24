package com.settle.tracker.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.settle.tracker.components.FabMenu
import com.settle.tracker.components.common.CoachMarkOverlay
import com.settle.tracker.components.common.CoachStep
import com.settle.tracker.components.common.ExpenseListSkeleton
import com.settle.tracker.components.common.FabOverlay
import com.settle.tracker.components.expenses.ExpenseTable
import com.settle.tracker.components.expenses.RecurringExpensesList
import com.settle.tracker.scheme.ExpenseScheme
import com.settle.tracker.ui.theme.BrandBlue
import com.settle.tracker.ui.theme.BrandTeal
import com.settle.tracker.utils.SettlePrefs

enum class DashboardType {
    EXPENSES,
    RECURRING_EXPENSES;

    fun getDisplayName(): String =
        name.lowercase().split('_', ' ').joinToString(" ") { w ->
            w.replaceFirstChar { it.uppercase() }
        }
}

@Composable
fun ExpensesScreen(
    onAddExpense: () -> Unit,
    onEditExpense: (String, String?) -> Unit,
    currentUser: FirebaseUser
) {
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val db = Firebase.firestore
    val prefs = remember { SettlePrefs(context.applicationContext) }

    var expenses by remember { mutableStateOf<List<ExpenseScheme>>(emptyList()) }
    var isFirstLoad by remember { mutableStateOf(true) }
    var expanded by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(DashboardType.EXPENSES) }

    var showCoach by remember { mutableStateOf(prefs.isFirstRun(SettlePrefs.TUTORIAL_EXPENSES)) }

    val onDeleteExpense: (String) -> Unit = { expenseId ->
        db.collection("users").document(currentUser.uid)
            .collection("expenses").document(expenseId).delete()
            .addOnFailureListener { Log.e("Firestore", "${it.message}") }
    }

    LaunchedEffect(Unit) {
        db.collection("users").document(currentUser.uid)
            .collection("expenses")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapShot, error ->
                if (error != null) {
                    Log.e("Firestore", "${error.message}")
                    isFirstLoad = false
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
                    isFirstLoad = false
                }
            }
    }

    val totalThisMonth = remember(expenses) {
        val now = java.util.Calendar.getInstance()
        val m = now.get(java.util.Calendar.MONTH)
        val y = now.get(java.util.Calendar.YEAR)
        expenses.filter {
            val c = java.util.Calendar.getInstance()
            c.timeInMillis = it.timestamp
            c.get(java.util.Calendar.MONTH) == m && c.get(java.util.Calendar.YEAR) == y
        }.sumOf { it.amount }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0),
        floatingActionButton = {
            FabMenu(
                expanded = expanded,
                onToggleExpanded = { expanded = !expanded },
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
                ) { focusManager.clearFocus() },
        ) {
            GreetingHero(
                name = currentUser.displayName ?: "there",
                totalThisMonth = totalThisMonth
            )

            // Tab row – custom pill
            TabSwitcher(
                selected = selectedTab,
                onSelect = { selectedTab = it }
            )

            Spacer(Modifier.height(4.dp))

            if (selectedTab == DashboardType.EXPENSES) {
                when {
                    isFirstLoad -> ExpenseListSkeleton()
                    expenses.isEmpty() -> EmptyExpensesState()
                    else -> ExpenseTable(
                        expenses = expenses,
                        onEditExpense = onEditExpense,
                        onDeleteExpense = onDeleteExpense,
                        modifier = Modifier
                    )
                }
            } else {
                RecurringExpensesList(
                    ownerCollection = "users",
                    ownerId = currentUser.uid,
                )
            }
        }

        FabOverlay(visible = expanded, onDismiss = { expanded = false })

        CoachMarkOverlay(
            visible = showCoach && !isFirstLoad,
            title = "Welcome to Settle!",
            steps = listOf(
                CoachStep(
                    icon = androidx.compose.material.icons.Icons.Filled.Add,
                    title = "Tap the + button",
                    body = "See the round teal button at the bottom-right? Tap it to log a new expense. You'll also see an option to import from a bank SMS."
                ),
                CoachStep(
                    icon = androidx.compose.material.icons.Icons.Filled.TouchApp,
                    title = "Long-press to delete",
                    body = "Press and hold on any expense row to quickly delete it. A confirmation dialog will appear."
                ),
                CoachStep(
                    icon = androidx.compose.material.icons.Icons.Filled.Repeat,
                    title = "Recurring expenses",
                    body = "Switch to the \"Recurring\" tab at the top to set up subscriptions, rent, or anything you pay on a schedule."
                ),
            ),
            onDismiss = {
                showCoach = false
                prefs.markSeen(SettlePrefs.TUTORIAL_EXPENSES)
            }
        )
    }
}

@Composable
private fun GreetingHero(name: String, totalThisMonth: Double) {
    Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.horizontalGradient(listOf(BrandTeal, BrandBlue))
                )
                .padding(horizontal = 18.dp, vertical = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Hey ${name.split(" ").first()} 👋",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Spent this month",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                    )
                    Text(
                        com.settle.tracker.utils.formatCurrency(totalThisMonth),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )
                }
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Savings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
}

@Composable
private fun TabSwitcher(
    selected: DashboardType,
    onSelect: (DashboardType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        DashboardType.entries.forEach { type ->
            val active = selected == type
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(13.dp))
                    .background(
                        if (active)
                            Brush.horizontalGradient(listOf(BrandTeal, BrandBlue))
                        else
                            Brush.horizontalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                    )
                    .clickable { onSelect(type) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = type.getDisplayName(),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (active) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun EmptyExpensesState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            BrandTeal.copy(alpha = 0.18f),
                            BrandBlue.copy(alpha = 0.18f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Savings,
                contentDescription = null,
                tint = BrandTeal,
                modifier = Modifier.size(44.dp)
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(
            "No expenses yet",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Tap the + button to log your first expense, or let us pick it up automatically from your bank SMS.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
