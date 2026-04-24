package com.settle.tracker.components.analytics.groups

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.settle.tracker.components.analytics.CategorizedSpendingChart
import com.settle.tracker.components.analytics.ChartCard
import com.settle.tracker.components.analytics.MonthlySpendingChart
import com.settle.tracker.components.analytics.TopSpenders
import com.settle.tracker.components.common.EmptyState
import com.settle.tracker.scheme.ExpenseScheme
import com.settle.tracker.scheme.GroupScheme

@Composable
fun GroupAnalytics() {
    val db = Firebase.firestore
    val currentUser = Firebase.auth.currentUser

    var groups by remember { mutableStateOf(emptyList<GroupScheme>()) }
    var selectedGroup by remember { mutableStateOf<GroupScheme?>(null) }
    var expenses by remember { mutableStateOf(emptyList<ExpenseScheme>()) }
    var groupsLoaded by remember { mutableStateOf(false) }
    var expensesLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (currentUser == null) return@LaunchedEffect
        db.collection("groups")
            .whereArrayContains("members", currentUser.uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) { groupsLoaded = true; return@addSnapshotListener }
                groups = snapshot.toObjects(GroupScheme::class.java)
                if (groups.isNotEmpty() && selectedGroup == null) selectedGroup = groups.first()
                groupsLoaded = true
            }
    }

    LaunchedEffect(selectedGroup) {
        if (selectedGroup == null) return@LaunchedEffect
        expensesLoaded = false
        db.collection("groups").document(selectedGroup!!.id).collection("expenses")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("Firestore", "${error.message}")
                    expensesLoaded = true
                    return@addSnapshotListener
                }
                if (snapshot == null) { expensesLoaded = true; return@addSnapshotListener }
                expenses = snapshot.toObjects(ExpenseScheme::class.java)
                expensesLoaded = true
            }
    }

    if (groupsLoaded && groups.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.Groups,
            title = "No groups to analyse",
            body = "Create a group from the Groups tab — once you log a few expenses, this space will come alive with beautiful charts."
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
            LazyRow(
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
            ) {
                items(groups, key = { it.id }) { group ->
                    FilterChip(
                        selected = selectedGroup?.id == group.id,
                        onClick = { selectedGroup = group },
                        label = { Text(group.groupName) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
        }

        if (expensesLoaded && expenses.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Filled.QueryStats,
                    title = "No data in this group yet",
                    body = "Log a few expenses in ${selectedGroup?.groupName ?: "this group"} to see charts & top spenders."
                )
            }
            return@LazyColumn
        }

        item {
            ChartCard(
                title = "Monthly Spends",
                subtitle = selectedGroup?.groupName ?: "",
                icon = Icons.Filled.CalendarMonth
            ) { MonthlySpendingChart(expenses) }
        }
        item {
            ChartCard(
                title = "Top Spenders",
                subtitle = "Who paid the most",
                icon = Icons.Filled.EmojiEvents
            ) { TopSpenders(expenses) }
        }
        item {
            ChartCard(
                title = "Categorized Spends",
                subtitle = "Group-wide breakdown",
                icon = Icons.Filled.Category
            ) { CategorizedSpendingChart(expenses) }
        }
    }
}
