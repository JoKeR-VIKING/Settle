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
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.settle.tracker.components.FabMenu
import com.settle.tracker.components.common.CoachMarkOverlay
import com.settle.tracker.components.common.CoachStep
import com.settle.tracker.components.common.ExpenseListSkeleton
import com.settle.tracker.components.common.FabOverlay
import com.settle.tracker.components.expenses.ExpenseSearchBar
import com.settle.tracker.components.expenses.ExpenseTable
import com.settle.tracker.components.expenses.RecurringExpensesList
import com.settle.tracker.scheme.ExpenseCategory
import com.settle.tracker.scheme.ExpenseScheme
import com.settle.tracker.scheme.GroupScheme
import com.settle.tracker.scheme.UserScheme
import com.settle.tracker.ui.animations.ShimmerBox
import com.settle.tracker.ui.theme.BrandBlue
import com.settle.tracker.ui.theme.BrandTeal
import com.settle.tracker.ui.theme.Warning
import com.settle.tracker.utils.SettlePrefs
import com.settle.tracker.utils.filterBySearch
import com.settle.tracker.utils.formatCurrency
import java.util.Calendar

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
    var searchQuery by remember { mutableStateOf("") }

    var userScheme by remember { mutableStateOf<UserScheme?>(null) }
    var showCoach by remember { mutableStateOf(false) }

    // Group expenses this user belongs to, kept only to fold "my share" into the
    // "Spent this month" total on the hero card.
    var isGroupsQueryLoaded by remember { mutableStateOf(false) }
    var knownGroupIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    val groupExpensesByGroup = remember { mutableStateMapOf<String, List<ExpenseScheme>>() }
    val groupExpenseListeners = remember { mutableStateMapOf<String, ListenerRegistration>() }

    val onDeleteExpense: (String) -> Unit = { expenseId ->
        db.collection("users").document(currentUser.uid)
            .collection("expenses").document(expenseId).delete()
            .addOnFailureListener { Log.e("Firestore", "${it.message}") }
    }

    LaunchedEffect(currentUser.uid) {
        db.collection("users").document(currentUser.uid)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    val scheme = snapshot.toObject(UserScheme::class.java)
                    userScheme = scheme
                    showCoach = scheme?.tourTaken == false && prefs.isFirstRun(SettlePrefs.TUTORIAL_EXPENSES)
                }
            }
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

    LaunchedEffect(currentUser.uid) {
        db.collection("groups")
            .whereArrayContains("members", currentUser.uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("Firestore", "${error.message}")
                    isGroupsQueryLoaded = true
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val groups = snapshot.toObjects(GroupScheme::class.java)
                    knownGroupIds = groups.map { it.id }.toSet()

                    groups.forEach { group ->
                        if (groupExpenseListeners.containsKey(group.id)) return@forEach

                        val registration = db.collection("groups").document(group.id)
                            .collection("expenses")
                            .addSnapshotListener { expenseSnap, expenseError ->
                                if (expenseError != null) {
                                    Log.e("Firestore", "${expenseError.message}")
                                    return@addSnapshotListener
                                }
                                groupExpensesByGroup[group.id] =
                                    expenseSnap?.toObjects(ExpenseScheme::class.java) ?: emptyList()
                            }
                        groupExpenseListeners[group.id] = registration
                    }
                }
                isGroupsQueryLoaded = true
            }
    }

    DisposableEffect(Unit) {
        onDispose {
            groupExpenseListeners.values.forEach { it.remove() }
            groupExpenseListeners.clear()
        }
    }

    fun isThisMonth(timestamp: Long): Boolean {
        val now = Calendar.getInstance()
        val c = Calendar.getInstance()
        c.timeInMillis = timestamp
        return c.get(Calendar.MONTH) == now.get(Calendar.MONTH) &&
            c.get(Calendar.YEAR) == now.get(Calendar.YEAR)
    }

    val personalTotalThisMonth = expenses
        .filter { it.category != ExpenseCategory.SETTLEMENT.name && isThisMonth(it.timestamp) }
        .sumOf { it.amount }

    // My share of each group expense, i.e. what I actually spent (excludes settlements,
    // which are balance transfers, not spend).
    val groupTotalThisMonth = groupExpensesByGroup.values.flatten()
        .filter { it.category != ExpenseCategory.SETTLEMENT.name && isThisMonth(it.timestamp) }
        .sumOf { expense ->
            expense.splits.firstOrNull { it.id == currentUser.uid }?.amount ?: 0.0
        }

    val isSpendLoaded = !isFirstLoad && isGroupsQueryLoaded &&
        knownGroupIds.all { groupExpensesByGroup.containsKey(it) }

    val combinedTotalThisMonth = personalTotalThisMonth + groupTotalThisMonth

    // Simple run-rate projection: at the current daily average, this is roughly
    // where the month ends up. Used to give an early warning, not a hard number.
    val now = Calendar.getInstance()
    val daysElapsed = now.get(Calendar.DAY_OF_MONTH)
    val daysInMonth = now.getActualMaximum(Calendar.DAY_OF_MONTH)
    val projectedTotalThisMonth = if (daysElapsed <= 0) combinedTotalThisMonth
        else (combinedTotalThisMonth / daysElapsed) * daysInMonth

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
                isLoading = !isSpendLoaded,
                personalTotal = personalTotalThisMonth,
                groupTotal = groupTotalThisMonth,
                combinedTotal = combinedTotalThisMonth,
                projectedTotal = projectedTotalThisMonth
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
                    else -> {
                        ExpenseSearchBar(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it }
                        )
                        Spacer(Modifier.height(4.dp))
                        val filteredExpenses = expenses.filterBySearch(searchQuery)
                        if (filteredExpenses.isEmpty()) {
                            NoSearchResultsState(query = searchQuery)
                        } else {
                            ExpenseTable(
                                expenses = filteredExpenses,
                                onEditExpense = onEditExpense,
                                onDeleteExpense = onDeleteExpense,
                                modifier = Modifier
                            )
                        }
                    }
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
                    icon = Icons.Filled.Add,
                    title = "Tap the + button",
                    body = "See the round teal button at the bottom-right? Tap it to log a new expense. You'll also see an option to import from a bank SMS."
                ),
                CoachStep(
                    icon = Icons.Filled.TouchApp,
                    title = "Long-press to delete",
                    body = "Press and hold on any expense row to quickly delete it. A confirmation dialog will appear."
                ),
                CoachStep(
                    icon = Icons.Filled.Repeat,
                    title = "Recurring expenses",
                    body = "Switch to the \"Recurring\" tab at the top to set up subscriptions, rent, or anything you pay on a schedule."
                ),
            ),
            onDismiss = {
                showCoach = false
                prefs.markSeen(SettlePrefs.TUTORIAL_EXPENSES)
                if (prefs.allToursSeen()) {
                    db.collection("users").document(currentUser.uid).update("tourTaken", true)
                }
            }
        )
    }
}

@Composable
private fun GreetingHero(
    name: String,
    isLoading: Boolean,
    personalTotal: Double,
    groupTotal: Double,
    combinedTotal: Double,
    projectedTotal: Double
) {
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
            Column(modifier = Modifier.fillMaxWidth()) {
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

                        if (isLoading) {
                            Spacer(Modifier.height(6.dp))
                            ShimmerBox(
                                baseColor = Color.White.copy(alpha = 0.20f),
                                highlightColor = Color.White.copy(alpha = 0.45f),
                                width = 110.dp,
                                height = 30.dp,
                                radius = 8.dp
                            )
                        } else {
                            Text(
                                formatCurrency(combinedTotal),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1
                            )
                        }
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

                if (isLoading) {
                    Spacer(Modifier.height(10.dp))
                    ShimmerBox(
                        baseColor = Color.White.copy(alpha = 0.20f),
                        highlightColor = Color.White.copy(alpha = 0.45f),
                        width = 190.dp,
                        height = 14.dp,
                        radius = 6.dp
                    )
                } else {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "${formatCurrency(personalTotal, compact = true).let { "₹$it" }} personal" +
                            " + ${formatCurrency(groupTotal, compact = true).let { "₹$it" }} group",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                        maxLines = 1
                    )

                    if (combinedTotal > 0 && projectedTotal > combinedTotal + 0.5) {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Warning.copy(alpha = 0.92f))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.TrendingUp,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "On pace for ${formatCurrency(projectedTotal)} this month",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }
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
fun NoSearchResultsState(query: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "No expenses match \"$query\"",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
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
