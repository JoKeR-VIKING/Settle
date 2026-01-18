package com.settle.tracker.screens

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.firebase.Firebase
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.settle.tracker.components.FabMenu
import com.settle.tracker.components.LoadingScreenWrapper
import com.settle.tracker.components.common.FabOverlay
import com.settle.tracker.components.expenses.ExpenseTable
import com.settle.tracker.components.groups.FullScreenDialog
import com.settle.tracker.components.groups.GroupTabRow
import com.settle.tracker.scheme.ExpenseScheme
import com.settle.tracker.scheme.GroupScheme
import com.settle.tracker.scheme.PaidBy

enum class GroupTab {
    EXPENSES,
    BALANCES
}

@Composable
fun GroupExpensesScreen(
    groupId: String,
    onBack: () -> Unit,
    onAddExpense: () -> Unit,
    onEditExpense: (String, String?) -> Unit
) {
    val focusManager = LocalFocusManager.current

    val db = Firebase.firestore

    var groupData by remember { mutableStateOf(GroupScheme()) }
    var isFetching by remember { mutableStateOf(false) }
    var showGroupDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(GroupTab.EXPENSES) }

    var expenses by remember { mutableStateOf<List<ExpenseScheme>>(emptyList()) }
    var expanded by remember { mutableStateOf(false) }

    val onDeleteExpense: (String) -> Unit = { expenseId ->
        isFetching = true

        db
            .collection("groups")
            .document(groupId)
            .collection("expenses")
            .document(expenseId)
            .delete()
            .addOnSuccessListener {
                isFetching = false
            }
            .addOnFailureListener {
                Log.e("Firestore", "${it.message}")
                isFetching = false
            }
    }

    LaunchedEffect(Unit) {
        isFetching = true

        db
            .collection("groups")
            .document(groupId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("Firestore", "${error.message}")
                    isFetching = false
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val data = snapshot.toObject(GroupScheme::class.java)
                    groupData = data ?: GroupScheme()
                    isFetching = false
                }
            }
    }

    LaunchedEffect(groupId) {
        isFetching = true

        db
            .collection("groups")
            .document(groupId)
            .collection("expenses")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapShot, error ->
                if (error != null) {
                    Log.e("Firestore", "${error.message}")
                    isFetching = false
                    return@addSnapshotListener
                }

                if (snapShot != null) {
                    expenses = snapShot.documents.mapNotNull { doc ->
                        val paidByMap = doc.get("paidBy") as? Map<*, *>
                        val paidBy = paidByMap?.let {
                            PaidBy(
                                id = it["id"] as? String ?: "",
                                name = it["name"] as? String ?: ""
                            )
                        }

                        ExpenseScheme(
                            id = doc.getString("id") ?: "",
                            timestamp = doc.getLong("timestamp") ?: 0L,
                            details = doc.getString("details") ?: "",
                            amount = doc.getDouble("amount") ?: 0.0,
                            category = doc.getString("category") ?: "",
                            paidBy = paidBy,
                            createdAt = doc.getLong("createdAt") ?: 0L
                        )
                    }
                }

                isFetching = false
            }
    }

    LoadingScreenWrapper(
        isFetching,
        "Fetching group data..."
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack
                    ) {
                        Icon(
                            modifier = Modifier.size(30.dp),
                            imageVector = Icons.Filled.ChevronLeft,
                            contentDescription = "Go Back"
                        )
                    }

                    Text(
                        groupData.groupName,
                        style = MaterialTheme.typography.bodyLarge,
                    )

                    IconButton(
                        onClick = { showGroupDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Go Back"
                        )
                    }
                }

                GroupTabRow(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it }
                )

                if (selectedTab == GroupTab.EXPENSES) {
                    Scaffold(
                        modifier = Modifier
                            .fillMaxSize(),
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
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    focusManager.clearFocus()
                                },
                        ) {
                            ExpenseTable(
                                expenses = expenses,
                                onEditExpense = onEditExpense,
                                onDeleteExpense = onDeleteExpense,
                                modifier = Modifier
                            )
                        }

                        FabOverlay(
                            visible = expanded,
                            onDismiss = { expanded = false }
                        )
                    }
                } else {
                    Text("Balances")
                }
            }

            if (showGroupDialog) {
                Dialog(
                    onDismissRequest = { showGroupDialog = false },
                    properties = DialogProperties(
                        usePlatformDefaultWidth = false
                    )
                ) {
                    FullScreenDialog(
                        onDismiss = { showGroupDialog = false },
                        groupData = groupData,
                        onBack = onBack
                    )
                }
            }
        }
    }
}
