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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.settle.tracker.components.expenses.ExpenseFilterSheet
import com.settle.tracker.components.expenses.ExpenseFilters
import com.settle.tracker.components.expenses.ExpenseTable
import com.settle.tracker.components.expenses.NoSearchResults
import com.settle.tracker.components.expenses.SearchFilterBar
import com.settle.tracker.components.expenses.filterBySearchAndFilters
import com.settle.tracker.ui.animations.ShimmerBox
import com.settle.tracker.ui.theme.Warning
import com.settle.tracker.components.expenses.RecurringExpensesList
import com.settle.tracker.scheme.ExpenseScheme
import com.settle.tracker.scheme.UserScheme
import com.settle.tracker.ui.theme.BrandBlue
import com.settle.tracker.ui.theme.BrandTeal
import com.settle.tracker.utils.SettlePrefs
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

@OptIn(ExperimentalMaterial3Api::class)
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

    var userScheme by remember { mutableStateOf<UserScheme?>(null) }
    var showCoach by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var activeFilters by remember { mutableStateOf(ExpenseFilters()) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var groupShareThisMonth by remember { mutableStateOf(0.0) }
    var groupShareLoaded by remember { mutableStateOf(false) }

    val filterSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
            .get()
            .addOnSuccessListener { groupSnapshot ->
                val groupIds = groupSnapshot.documents.map { it.id }
                if (groupIds.isEmpty()) {
                    groupShareThisMonth = 0.0
                    groupShareLoaded = true
                    return@addOnSuccessListener
                }

                val monthStart = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                val pending = intArrayOf(groupIds.size)
                val shareTotal = doubleArrayOf(0.0)

                groupIds.forEach { gId ->
                    db.collection("groups").document(gId)
                        .collection("expenses")
                        .whereGreaterThanOrEqualTo("timestamp", monthStart)
                        .get()
                        .addOnSuccessListener { expSnap ->
                            shareTotal[0] += expSnap.documents.sumOf { doc ->
                                // Settlements aren't spending — they just move money
                                // between members, so exclude them from "spent".
                                if (doc.getString("category") == "SETTLEMENT") return@sumOf 0.0

                                val splits = doc.get("splits") as? List<*> ?: emptyList<Any>()
                                splits.filterIsInstance<Map<*, *>>()
                                    .firstOrNull { it["id"] == currentUser.uid }
                                    ?.let { (it["amount"] as? Number)?.toDouble() }
                                    ?: 0.0
                            }
                            pending[0]--
                            if (pending[0] == 0) {
                                groupShareThisMonth = shareTotal[0]
                                groupShareLoaded = true
                            }
                        }
                        .addOnFailureListener {
                            pending[0]--
                            if (pending[0] == 0) {
                                groupShareThisMonth = shareTotal[0]
                                groupShareLoaded = true
                            }
                        }
                }
            }
            .addOnFailureListener {
                groupShareThisMonth = 0.0
                groupShareLoaded = true
            }
    }

    val totalThisMonth = remember(expenses) {
        val now = Calendar.getInstance()
        val m = now.get(Calendar.MONTH)
        val y = now.get(Calendar.YEAR)
        expenses.filter {
            val c = Calendar.getInstance()
            c.timeInMillis = it.timestamp
            c.get(Calendar.MONTH) == m && c.get(Calendar.YEAR) == y
        }.sumOf { it.amount }
    }

    val filteredExpenses = remember(expenses, searchQuery, activeFilters) {
        expenses.filterBySearchAndFilters(searchQuery, activeFilters)
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
                personalThisMonth = totalThisMonth,
                groupShareThisMonth = groupShareThisMonth,
                totalsReady = !isFirstLoad && groupShareLoaded
            )

            TabSwitcher(
                selected = selectedTab,
                onSelect = { selectedTab = it }
            )

            Spacer(Modifier.height(4.dp))

            if (selectedTab == DashboardType.EXPENSES) {
                SearchFilterBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    hasActiveFilters = activeFilters.isActive,
                    onFilterClick = { showFilterSheet = true }
                )

                when {
                    isFirstLoad -> ExpenseListSkeleton()
                    expenses.isEmpty() -> EmptyExpensesState()
                    filteredExpenses.isEmpty() -> NoSearchResults(
                        query = searchQuery,
                        hasFilters = activeFilters.isActive
                    )
                    else -> ExpenseTable(
                        expenses = filteredExpenses,
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

        if (showFilterSheet) {
            ExpenseFilterSheet(
                sheetState = filterSheetState,
                filters = activeFilters,
                onFiltersChanged = { activeFilters = it },
                onDismiss = { showFilterSheet = false }
            )
        }
    }
}

@Composable
private fun GreetingHero(
    name: String,
    personalThisMonth: Double,
    groupShareThisMonth: Double,
    totalsReady: Boolean
) {
    val combinedTotal = personalThisMonth + groupShareThisMonth
    val cal = Calendar.getInstance()
    val daysElapsed = cal.get(Calendar.DAY_OF_MONTH)
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val dailyRate = if (daysElapsed > 0) combinedTotal / daysElapsed else 0.0
    val projectedMonthEnd = dailyRate * daysInMonth

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Text(
            text = "Hey, ${name.split(" ").first()} 👋",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))
        if (!totalsReady) {
            GreetingHeroSkeleton()
        } else {
            Text(
                text = formatCurrency(combinedTotal),
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "spent this month",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            SpendSplitRow(
                personalThisMonth = personalThisMonth,
                groupShareThisMonth = groupShareThisMonth
            )
            if (combinedTotal > 0 && daysElapsed > 0) {
                Spacer(Modifier.height(12.dp))
                PaceWarningBanner(
                    dailyRate = dailyRate,
                    projectedMonthEnd = projectedMonthEnd
                )
            }
        }
    }
}

@Composable
private fun GreetingHeroSkeleton() {
    val base = MaterialTheme.colorScheme.surfaceVariant
    val hl = MaterialTheme.colorScheme.surface
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ShimmerBox(baseColor = base, highlightColor = hl, width = 168.dp, height = 32.dp, radius = 10.dp)
        ShimmerBox(baseColor = base, highlightColor = hl, width = 108.dp, height = 12.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ShimmerBox(baseColor = base, highlightColor = hl, width = 128.dp, height = 28.dp, radius = 10.dp)
            ShimmerBox(baseColor = base, highlightColor = hl, width = 118.dp, height = 28.dp, radius = 10.dp)
        }
        ShimmerBox(
            baseColor = base,
            highlightColor = hl,
            modifier = Modifier.fillMaxWidth(),
            height = 52.dp,
            radius = 14.dp
        )
    }
}

@Composable
private fun SpendSplitRow(
    personalThisMonth: Double,
    groupShareThisMonth: Double
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SplitChip(
            label = "Personal",
            amount = personalThisMonth,
            modifier = Modifier.weight(1f)
        )
        SplitChip(
            label = "Groups",
            amount = groupShareThisMonth,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SplitChip(
    label: String,
    amount: Double,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = formatCurrency(amount),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun PaceWarningBanner(
    dailyRate: Double,
    projectedMonthEnd: Double
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Warning.copy(alpha = 0.16f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = null,
            tint = Warning,
            modifier = Modifier.size(22.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "On pace for ${formatCurrency(projectedMonthEnd)}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${formatCurrency(dailyRate)}/day so far this month",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
                        if (active) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
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

