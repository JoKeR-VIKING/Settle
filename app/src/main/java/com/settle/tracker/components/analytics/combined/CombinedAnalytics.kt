package com.settle.tracker.components.analytics.combined

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Storefront
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
import com.settle.tracker.components.analytics.ChartCard
import com.settle.tracker.components.analytics.PaymentMethodChart
import com.settle.tracker.components.analytics.VendorSpendChart
import com.settle.tracker.components.common.EmptyState
import com.settle.tracker.scheme.ExpenseScheme

/**
 * "Everything" analytics — combines the user's personal expenses with their
 * share of every group they belong to.
 *
 * Two different bases are used depending on the question:
 *  - Payment methods: money that actually left the user's accounts, i.e. personal
 *    (full amount) + the user's own `paidBy` amount in group expenses. Both keep
 *    the expense's `paidFrom`.
 *  - Vendors: what the user consumed, i.e. personal (full amount) + the user's
 *    `splits` share in group expenses.
 */
@Composable
fun CombinedAnalytics() {
    val db = Firebase.firestore
    val currentUser = Firebase.auth.currentUser
    val uid = currentUser?.uid

    var personalExpenses by remember { mutableStateOf(emptyList<ExpenseScheme>()) }
    var groupExpenses by remember { mutableStateOf(emptyList<ExpenseScheme>()) }
    var personalLoaded by remember { mutableStateOf(false) }
    var groupsLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(uid) {
        if (uid == null) return@LaunchedEffect
        db.collection("users").document(uid).collection("expenses")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    Log.e("Firestore", "${error?.message}")
                    personalLoaded = true
                    return@addSnapshotListener
                }
                personalExpenses = snapshot.toObjects(ExpenseScheme::class.java)
                personalLoaded = true
            }
    }

    LaunchedEffect(uid) {
        if (uid == null) return@LaunchedEffect
        db.collection("groups")
            .whereArrayContains("members", uid)
            .get()
            .addOnSuccessListener { groupSnapshot ->
                val groupIds = groupSnapshot.documents.map { it.id }
                if (groupIds.isEmpty()) {
                    groupsLoaded = true
                    return@addOnSuccessListener
                }

                val collected = mutableListOf<ExpenseScheme>()
                var pending = groupIds.size

                groupIds.forEach { gId ->
                    db.collection("groups").document(gId).collection("expenses")
                        .get()
                        .addOnSuccessListener { expSnap ->
                            collected.addAll(expSnap.toObjects(ExpenseScheme::class.java))
                            pending--
                            if (pending == 0) {
                                groupExpenses = collected.toList()
                                groupsLoaded = true
                            }
                        }
                        .addOnFailureListener {
                            pending--
                            if (pending == 0) {
                                groupExpenses = collected.toList()
                                groupsLoaded = true
                            }
                        }
                }
            }
            .addOnFailureListener {
                groupsLoaded = true
            }
    }

    val paymentMethodExpenses = remember(personalExpenses, groupExpenses, uid) {
        val fromGroups = groupExpenses.mapNotNull { expense ->
            val myPaid = expense.paidBy.firstOrNull { it.id == uid }?.amount ?: 0.0
            if (myPaid <= 0.0 || expense.paidFrom.isBlank()) null
            else expense.copy(amount = myPaid)
        }
        personalExpenses + fromGroups
    }

    val vendorExpenses = remember(personalExpenses, groupExpenses, uid) {
        val fromGroups = groupExpenses.mapNotNull { expense ->
            val myShare = expense.splits.firstOrNull { it.id == uid }?.amount ?: 0.0
            if (myShare <= 0.0) null else expense.copy(amount = myShare)
        }
        personalExpenses + fromGroups
    }

    val loaded = personalLoaded && groupsLoaded

    if (loaded && personalExpenses.isEmpty() && groupExpenses.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.QueryStats,
            title = "Nothing to combine yet",
            body = "Once you log personal expenses or spend in a group, this space blends both into a single view."
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
                title = "Payment Methods",
                subtitle = "Across personal & all groups",
                icon = Icons.Filled.AccountBalanceWallet
            ) { PaymentMethodChart(paymentMethodExpenses) }
        }
        item {
            ChartCard(
                title = "Popular Vendors",
                subtitle = "Zomato, Swiggy, Amazon & more",
                icon = Icons.Filled.Storefront
            ) { VendorSpendChart(vendorExpenses) }
        }
    }
}
