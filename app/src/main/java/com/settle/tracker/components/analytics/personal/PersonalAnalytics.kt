package com.settle.tracker.components.analytics.personal

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.settle.tracker.components.analytics.CategorizedSpendingChart
import com.settle.tracker.components.analytics.ChartCard
import com.settle.tracker.components.analytics.DailySpendingChart
import com.settle.tracker.components.analytics.MonthlySpendingChart
import com.settle.tracker.components.common.EmptyState
import com.settle.tracker.scheme.ExpenseScheme

@Composable
fun PersonalAnalytics() {
    val db = Firebase.firestore
    val currentUser = Firebase.auth.currentUser

    var expenses by remember { mutableStateOf(emptyList<ExpenseScheme>()) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (currentUser == null) return@LaunchedEffect
        db.collection("users").document(currentUser.uid).collection("expenses")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("Firestore", "${error.message}")
                    loaded = true
                    return@addSnapshotListener
                }
                if (snapshot == null) { loaded = true; return@addSnapshotListener }
                expenses = snapshot.toObjects(ExpenseScheme::class.java)
                loaded = true
            }
    }

    if (loaded && expenses.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.QueryStats,
            title = "No insights yet",
            body = "Add a few expenses to unlock spending charts, monthly trends, and category breakdowns."
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ChartCard(
                title = "Monthly Spends",
                subtitle = "Trend across recent months",
                icon = Icons.Filled.CalendarMonth
            ) { MonthlySpendingChart(expenses) }
        }
        item {
            ChartCard(
                title = "Daily Spends",
                subtitle = "Day-by-day breakdown",
                icon = Icons.Filled.Timeline
            ) { DailySpendingChart(expenses) }
        }
        item {
            ChartCard(
                title = "Categorized Spends",
                subtitle = "Where your money goes",
                icon = Icons.Filled.Category
            ) { CategorizedSpendingChart(expenses) }
        }
    }
}
