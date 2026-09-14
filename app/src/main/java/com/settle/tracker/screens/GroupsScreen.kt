package com.settle.tracker.screens

import android.util.Log
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.settle.tracker.components.common.CoachMarkOverlay
import com.settle.tracker.components.common.CoachStep
import com.settle.tracker.components.common.GroupRowSkeleton
import com.settle.tracker.components.groups.CreateGroupModal
import com.settle.tracker.scheme.ExpenseScheme
import com.settle.tracker.scheme.GroupScheme
import com.settle.tracker.scheme.UserScheme
import com.settle.tracker.ui.animations.bounceClickable
import com.settle.tracker.ui.theme.BrandTeal
import com.settle.tracker.ui.theme.BrandBlue
import com.settle.tracker.ui.theme.Success
import com.settle.tracker.utils.SettlePrefs
import com.settle.tracker.utils.calculateNetBalances
import com.settle.tracker.utils.formatCurrency
import java.util.UUID
import kotlin.math.absoluteValue

const val MAX_GROUP_NAME_CHARS = 20

data class GroupWithBalance(
    val group: GroupScheme,
    val balance: Double,
    val lastActivityAt: Long = 0L
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(
    currentUser: FirebaseUser,
    onOpenGroup: (String) -> Unit
) {
    val groupModalSheetState = rememberModalBottomSheetState()
    val context = LocalContext.current
    val prefs = remember { SettlePrefs(context.applicationContext) }

    val db = Firebase.firestore

    var groupsWithBalance by remember { mutableStateOf<Map<String, GroupWithBalance>>(emptyMap()) }
    val groupListener = remember { mutableStateMapOf<String, ListenerRegistration>() }
    var showGroupModal by remember { mutableStateOf(false) }
    var isFetching by remember { mutableStateOf(true) }
    var isSubmitting by remember { mutableStateOf(false) }

    var groupId by remember { mutableStateOf(UUID.randomUUID().toString()) }
    var newGroupName by remember { mutableStateOf(TextFieldValue("")) }

    var userScheme by remember { mutableStateOf<UserScheme?>(null) }
    var showCoach by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

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
            .addOnFailureListener { isSubmitting = false }
    }

    LaunchedEffect(currentUser.uid) {
        db.collection("users").document(currentUser.uid)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    val scheme = snapshot.toObject(UserScheme::class.java)
                    userScheme = scheme
                    showCoach = scheme?.tourTaken == false && prefs.isFirstRun(SettlePrefs.TUTORIAL_GROUPS)
                }
            }
    }

    LaunchedEffect(Unit) {
        db.collection("groups")
            .whereArrayContains("members", currentUser.uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { Log.e("Firestore", "${error.message}"); isFetching = false; return@addSnapshotListener }
                if (snapshot == null) { isFetching = false; return@addSnapshotListener }
                val groups = snapshot.toObjects(GroupScheme::class.java)
                groups.forEach { group ->
                    if (groupListener.containsKey(group.id)) return@forEach
                    val registration = db.collection("groups").document(group.id).collection("expenses")
                        .addSnapshotListener { expenseSnap, e ->
                            if (e != null) { Log.e("Firestore", "${e.message}"); isFetching = false; return@addSnapshotListener }
                            val expenses = expenseSnap?.toObjects(ExpenseScheme::class.java) ?: emptyList()
                            val myBalance = calculateNetBalances(expenses)
                            val lastExpenseCreated = expenses.maxOfOrNull { it.createdAt } ?: 0L
                            groupsWithBalance = groupsWithBalance + (group.id to GroupWithBalance(
                                group = group,
                                balance = myBalance[currentUser.uid] ?: 0.0,
                                lastActivityAt = maxOf(group.createdAt, lastExpenseCreated)
                            ))
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

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GroupsHeader()

            AddGroupCard(onClick = { showGroupModal = true })

            when {
                isFetching -> Column {
                    repeat(4) { GroupRowSkeleton() }
                }
                groupsWithBalance.isEmpty() -> EmptyGroupsState()
                else -> {
                    if (groupsWithBalance.size > 3) {
                        GroupSearchBar(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it }
                        )
                    }

                    val sortedGroups = groupsWithBalance.values
                        .sortedByDescending { it.lastActivityAt }
                        .filter {
                            searchQuery.isBlank() ||
                                it.group.groupName.contains(searchQuery, ignoreCase = true)
                        }

                    if (sortedGroups.isEmpty()) {
                        Text(
                            text = "No groups matching \"$searchQuery\".",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp, vertical = 40.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 32.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(sortedGroups, key = { it.group.id }) { gb ->
                                GroupRowCard(gb = gb, onClick = { onOpenGroup(gb.group.id) })
                            }
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

        CoachMarkOverlay(
            visible = showCoach && !isFetching,
            title = "Your groups",
            steps = listOf(
                CoachStep(
                    icon = Icons.Filled.GroupAdd,
                    title = "Start a new group",
                    body = "Tap the \"Start a new group\" card at the top to create a shared expense group — like trips, flatmates, or a dinner night."
                ),
                CoachStep(
                    icon = Icons.Filled.Balance,
                    title = "See balances instantly",
                    body = "Each group shows whether you owe or are owed — green means you lent, red means you owe."
                ),
                CoachStep(
                    icon = Icons.Filled.TouchApp,
                    title = "Tap a group to open",
                    body = "Inside a group you can add expenses split across members, and settle up in one tap."
                ),
            ),
            onDismiss = {
                showCoach = false
                prefs.markSeen(SettlePrefs.TUTORIAL_GROUPS)
                if (prefs.allToursSeen()) {
                    db.collection("users").document(currentUser.uid).update("tourTaken", true)
                }
            }
        )
    }
}

@Composable
private fun GroupsHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Groups",
            style = MaterialTheme.typography.headlineMedium
        )
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.GroupAdd,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupSearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        value = query,
        onValueChange = onQueryChange,
        placeholder = {
            Text("Search groups...", style = MaterialTheme.typography.labelLarge)
        },
        leadingIcon = {
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
        },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Clear",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        } else null,
        shape = RoundedCornerShape(16.dp),
        singleLine = true,
        textStyle = MaterialTheme.typography.labelLarge,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
    )
}

@Composable
private fun AddGroupCard(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .bounceClickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                "New group",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "Trips, roommates, dinners…",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun GroupRowCard(
    gb: GroupWithBalance,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .bounceClickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Group,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                gb.group.groupName.ifBlank { "Untitled Group" },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = when {
                    gb.balance > 0.0 -> "You are owed"
                    gb.balance < 0.0 -> "You owe"
                    else             -> "All settled"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        Text(
            text = if (gb.balance == 0.0) "—" else formatCurrency(gb.balance.absoluteValue),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.3.sp,
            maxLines = 1,
            color = when {
                gb.balance > 0.0 -> Success
                gb.balance < 0.0 -> MaterialTheme.colorScheme.error
                else             -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

@Composable
private fun EmptyGroupsState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
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
                Icons.Filled.Group,
                contentDescription = null,
                tint = BrandTeal,
                modifier = Modifier.size(44.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text("No groups yet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            "Create your first group to start splitting bills with friends & family.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
