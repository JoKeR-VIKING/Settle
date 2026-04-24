package com.settle.tracker.components.analytics.personal

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import com.google.firebase.firestore.firestore
import com.settle.tracker.components.analytics.CategorizedSpendingChart
import com.settle.tracker.components.analytics.DailySpendingChart
import com.settle.tracker.components.analytics.MonthlySpendingChart
import com.settle.tracker.components.common.EmptyState
import com.settle.tracker.scheme.ExpenseScheme

@Composable
fun PersonalAnalytics() {
    val db = Firebase.firestore
    val currentUser = Firebase.auth.currentUser

    var expenses by remember { mutableStateOf(emptyList<ExpenseScheme>()) }

    LaunchedEffect(Unit) {
        if (currentUser == null) return@LaunchedEffect

        db
            .collection("users")
            .document(currentUser.uid)
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

    if (expenses.isEmpty()) {
        EmptyState(
            assetName = "empty_analytics.json",
            title = "No Data Yet",
            description = "Start adding expenses to see your spending patterns visualized here."
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
                    text = "Daily Spends",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            item {
                DailySpendingChart(expenses)
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
