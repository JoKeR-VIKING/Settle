package com.settle.tracker.components.analytics.groups

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
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

    LaunchedEffect(Unit) {
        if (currentUser == null) return@LaunchedEffect

        db
            .collection("groups")
            .whereArrayContains("members", currentUser.uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                groups = snapshot.toObjects(GroupScheme::class.java)
                if (groups.isNotEmpty()) selectedGroup = groups.first()
            }
    }

    LaunchedEffect(selectedGroup) {
        if (selectedGroup == null) return@LaunchedEffect

        db
            .collection("groups")
            .document(selectedGroup!!.id)
            .collection("expenses")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("Firestore", "${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot == null) return@addSnapshotListener

                expenses = snapshot.toObjects(ExpenseScheme::class.java)
            }
    }

    if (groups.isEmpty()) {
        EmptyState(
            assetName = "empty_groups.json",
            title = "No Groups Found",
            description = "You need to be part of at least one group to see group analytics."
        )
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, top = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(35.dp)
        ) {
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(groups, key = { it.id }) { group ->
                        FilterChip(
                            selected = selectedGroup?.id == group.id,
                            onClick = { selectedGroup = group },
                            label = { Text(group.groupName) }
                        )
                    }
                }
            }

            if (expenses.isEmpty()) {
                item {
                    EmptyState(
                        assetName = "empty_analytics.json",
                        title = "No Expenses in this Group",
                        description = "Add some group expenses to see the analytics for ${selectedGroup?.groupName}."
                    )
                }
            } else {
                item {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = "Monthly Spends",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                item {
                    MonthlySpendingChart(expenses)
                }

                item {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = "Top Spenders",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                item {
                    TopSpenders(expenses)
                }

                item {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = "Categorized Spends",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                item {
                    CategorizedSpendingChart(expenses)
                }
            }
        }
    }
}
