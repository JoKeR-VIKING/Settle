package com.settle.tracker.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.settle.tracker.components.FabMenu
import com.settle.tracker.components.expenses.ExpenseTable
import com.settle.tracker.scheme.ExpenseScheme

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
                .padding(top = 16.dp)
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

        if (expanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .pointerInput(Unit) {
                        detectTapGestures {
                            expanded = false
                        }
                    }
            )
        }
    }
}
