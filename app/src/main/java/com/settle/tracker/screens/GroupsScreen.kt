package com.settle.tracker.screens

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.settle.tracker.components.LoadingScreenWrapper
import com.settle.tracker.components.groups.CreateGroupModal
import com.settle.tracker.scheme.ExpenseScheme
import com.settle.tracker.scheme.GroupScheme
import com.settle.tracker.utils.calculateNetBalances
import com.settle.tracker.utils.dashedBorder
import com.settle.tracker.utils.formatCurrency
import java.util.UUID
import kotlin.math.absoluteValue

const val MAX_GROUP_NAME_CHARS = 20

data class GroupWithBalance(
    val group: GroupScheme,
    val balance: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(
    currentUser: FirebaseUser,
    onOpenGroup: (String) -> Unit
) {
    val groupModalSheetState = rememberModalBottomSheetState()

    val db = Firebase.firestore

    var groupsWithBalance by remember { mutableStateOf<Map<String, GroupWithBalance>>(emptyMap()) }
    val groupListener = remember { mutableStateMapOf<String, ListenerRegistration>() }
    var showGroupModal by remember { mutableStateOf(false) }
    var isFetching by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    var groupId by remember { mutableStateOf(UUID.randomUUID().toString()) }
    var newGroupName by remember { mutableStateOf(TextFieldValue("")) }

    fun createGroup() {
        isSubmitting = true

        val groupData = GroupScheme(
            id = groupId,
            groupName = newGroupName.text,
            members = listOf(
                currentUser.uid
            ),
            createdAt = System.currentTimeMillis(),
            createdBy = currentUser.uid
        )

        db
            .collection("groups")
            .document(groupId)
            .set(groupData)
            .addOnSuccessListener {
                isSubmitting = false
                showGroupModal = false
            }
            .addOnFailureListener {
                isSubmitting = false
            }
    }

    LaunchedEffect(Unit) {
        isFetching = true

        db
            .collection("groups")
            .whereArrayContains("members", currentUser.uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("Firestore", "${error.message}")
                    isFetching = false
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    isFetching = false
                    return@addSnapshotListener
                }

                val groups = snapshot.toObjects(GroupScheme::class.java)

                groups.forEach { group ->
                    if (groupListener.containsKey(group.id)) return@forEach

                    val registration =
                        db
                            .collection("groups")
                            .document(group.id)
                            .collection("expenses")
                            .addSnapshotListener { expenseSnap, e ->
                                if (e != null) {
                                    Log.e("Firestore", "${e.message}")
                                    isFetching = false
                                    return@addSnapshotListener
                                }

                                val expenses =
                                    expenseSnap?.toObjects(ExpenseScheme::class.java) ?: emptyList()
                                val myBalance = calculateNetBalances(expenses)

                                val updatedGroup = GroupWithBalance(
                                    group = group,
                                    balance = myBalance[currentUser.uid] ?: 0.0
                                )

                                groupsWithBalance =
                                    groupsWithBalance + (group.id to updatedGroup)
                            }

                    groupListener[group.id] = registration
                }

                isFetching = false
            }
    }

    DisposableEffect(Unit) {
        onDispose {
            groupListener.values.forEach { it.remove() }
            groupListener.clear()
        }
    }

    LoadingScreenWrapper(
        isLoading = isFetching,
        message = "Fetching groups..."
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(36.dp),
            ) {
                Text(
                    text = "Groups",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 16.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .dashedBorder(
                                2.dp,
                                MaterialTheme.colorScheme.primary,
                                cornerRadius = 20.dp,
                                dashLength = 30f,
                                gapLength = 15f
                            ),
                        contentPadding = PaddingValues(vertical = 14.dp),
                        border = BorderStroke(
                            width = 0.dp,
                            color = Color.Transparent
                        ),
                        shape = RoundedCornerShape(25),
                        onClick = { showGroupModal = true }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add New Group",
                            tint = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            "Add New",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        groupsWithBalance
                            .values
                            .sortedByDescending { it.group.createdAt }
                            .forEach { groupWithBalance ->
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable(
                                                onClick = { onOpenGroup(groupWithBalance.group.id) }
                                            )
                                            .padding(horizontal = 22.dp, vertical = 20.dp),
                                        horizontalArrangement = Arrangement.spacedBy(25.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Group,
                                            contentDescription = "Group Icon",
                                        )

                                        Text(
                                            groupWithBalance.group.groupName,
                                            style = MaterialTheme.typography.labelLarge
                                        )

                                        Column(
                                            modifier = Modifier.weight(1f),
                                            horizontalAlignment = Alignment.End,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = if (groupWithBalance.balance > 0.0) "You are owed" else "You owe",
                                                style = MaterialTheme.typography.labelSmall
                                            )

                                            Text(
                                                text = formatCurrency(groupWithBalance.balance.absoluteValue),
                                                style = MaterialTheme.typography.labelLarge,
                                                letterSpacing = 0.3.sp,
                                                color = (
                                                    if (groupWithBalance.balance > 0.0) MaterialTheme.colorScheme.surfaceBright
                                                    else if (groupWithBalance.balance < 0.0) MaterialTheme.colorScheme.error
                                                    else MaterialTheme.colorScheme.onSurface
                                                    ),
                                            )
                                        }
                                    }

                                    HorizontalDivider(color = Color.Gray.copy(0.4f))
                                }
                            }
                    }
                }
            }

            if (showGroupModal) {
                CreateGroupModal(
                    sheetState = groupModalSheetState,
                    onDismissRequest = { showGroupModal = false },
                    groupName = newGroupName,
                    onGroupNameChange = { newGroupName = it },
                    isSubmitting = isSubmitting,
                    onCreateGroup = { createGroup() }
                )
            }
        }
    }
}
