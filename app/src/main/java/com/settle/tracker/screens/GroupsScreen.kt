package com.settle.tracker.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.settle.tracker.components.LoadingScreenWrapper
import com.settle.tracker.components.common.ExpenseListSkeleton
import com.settle.tracker.components.common.EmptyState
import com.settle.tracker.components.common.ScreenHeader
import com.settle.tracker.components.groups.CreateGroupModal
import com.settle.tracker.scheme.ExpenseScheme
import com.settle.tracker.scheme.GroupScheme
import com.settle.tracker.ui.theme.Danger
import com.settle.tracker.ui.theme.Success
import com.settle.tracker.utils.calculateNetBalances
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
            members = listOf(currentUser.uid),
            createdAt = System.currentTimeMillis(),
            createdBy = currentUser.uid
        )

        db.collection("groups").document(groupId).set(groupData)
            .addOnSuccessListener {
                isSubmitting = false
                showGroupModal = false
                groupId = UUID.randomUUID().toString()
                newGroupName = TextFieldValue("")
            }
            .addOnFailureListener {
                isSubmitting = false
            }
    }

    LaunchedEffect(Unit) {
        isFetching = true
        db.collection("groups")
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
                    if (!groupListener.containsKey(group.id)) {
                        val registration = db.collection("groups")
                            .document(group.id)
                            .collection("expenses")
                            .addSnapshotListener { expenseSnap, e ->
                                if (e != null) return@addSnapshotListener
                                val expenses = expenseSnap?.toObjects(ExpenseScheme::class.java) ?: emptyList()
                                val myBalance = calculateNetBalances(expenses)
                                val updatedGroup = GroupWithBalance(
                                    group = group,
                                    balance = myBalance[currentUser.uid] ?: 0.0
                                )
                                groupsWithBalance = groupsWithBalance + (group.id to updatedGroup)
                            }
                        groupListener[group.id] = registration
                    }
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

    LoadingScreenWrapper(isLoading = isSubmitting, message = "Creating your group...") {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            ScreenHeader(
                title = "Shared groups",
                subtitle = "Split trips, dinners, and house costs without losing the thread."
            )

            if (isFetching && groupsWithBalance.isEmpty()) {
                ExpenseListSkeleton(modifier = Modifier.weight(1f))
            } else if (groupsWithBalance.isEmpty()) {
                Box(modifier = Modifier.weight(1f)) {
                    EmptyState(
                        assetName = "empty_groups.json",
                        title = "No groups found",
                        description = "Create a group to start splitting expenses with friends, roommates, or your travel crew."
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .clickable { showGroupModal = true },
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        ) {
                            Row(
                                modifier = Modifier.padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = "Create a new group",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    items(groupsWithBalance.values.sortedByDescending { it.group.createdAt }.toList()) { groupWithBalance ->
                        GroupCard(groupWithBalance, onOpenGroup)
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

@Composable
fun GroupCard(
    groupWithBalance: GroupWithBalance,
    onOpenGroup: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onOpenGroup(groupWithBalance.group.id) },
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Group,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = groupWithBalance.group.groupName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${groupWithBalance.group.members.size} members",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                val balance = groupWithBalance.balance
                Text(
                    text = formatCurrency(balance.absoluteValue),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        balance > 0 -> Success
                        balance < 0 -> Danger
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Text(
                    text = when {
                        balance > 0 -> "you are owed"
                        balance < 0 -> "you owe"
                        else -> "no balance"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
