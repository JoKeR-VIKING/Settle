package com.settle.tracker.components.groups

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import com.settle.tracker.components.ConfirmAlertDialog
import com.settle.tracker.components.LoadingScreenWrapper
import com.settle.tracker.scheme.GroupScheme
import com.settle.tracker.scheme.UserScheme
import com.settle.tracker.utils.fetchGroupMembersChunked
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenDialog(
    onDismiss: () -> Unit,
    groupData: GroupScheme,
    onBack: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val db = Firebase.firestore

    var groupName by remember { mutableStateOf(TextFieldValue(groupData.groupName)) }
    var groupMembers by remember { mutableStateOf(emptyList<UserScheme>()) }

    var isEditingGroupName by remember { mutableStateOf(false) }
    var isDeletingGroup by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    fun updateGroupName() {
        isEditingGroupName = true

        val groupRef = db
            .collection("groups")
            .document(groupData.id)

        groupRef
            .update("groupName", groupName.text)
            .addOnSuccessListener {
                isEditingGroupName = false
            }
            .addOnFailureListener {
                isEditingGroupName = false
            }
    }

    fun normalizePhoneNumber(raw: String): String? {
        val digitsOnly = raw.filter { it.isDigit() }
        val lastTenDigits = digitsOnly.takeLast(10)

        return if (lastTenDigits.length == 10) {
            "+91$lastTenDigits"
        } else {
            null
        }
    }

    fun getPhoneNumberFromUri(
        context: Context,
        uri: Uri
    ): String? {
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        return context.contentResolver.query(
            uri,
            projection,
            null,
            null,
            null
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return null
            normalizePhoneNumber(cursor.getString(0))
        }
    }

    fun addMemberToGroup(phoneNumber: String) {
        db
            .collection("users")
            .whereEqualTo("phoneNumber", phoneNumber)
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    val userId = snapshot.documents.first().id
                    if (groupData.members.contains(userId)) {
                        isLoading = false
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "User already in group",
                            )
                        }
                        return@addOnSuccessListener
                    }

                    db
                        .collection("groups")
                        .document(groupData.id)
                        .update("members", FieldValue.arrayUnion(userId))
                        .addOnSuccessListener {
                            isLoading = false
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "User added in group",
                                )
                            }
                        }
                        .addOnFailureListener {
                            Log.e("Firestore", "${it.message}")
                            isLoading = false
                        }
                } else {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = "User not found on Settle",
                        )
                    }
                    isLoading = false
                }
            }
            .addOnFailureListener {
                isLoading = false
            }
    }

    fun deleteGroup(groupId: String) {
        isLoading = true

        db
            .collection("groups")
            .document(groupId)
            .delete()
            .addOnSuccessListener {
                onBack()
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
            }
    }

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult

        val uri = result.data?.data ?: return@rememberLauncherForActivityResult
        val phoneNumber = getPhoneNumberFromUri(
            context,
            uri = uri
        )
        if (phoneNumber == null) return@rememberLauncherForActivityResult
        isLoading = true

        addMemberToGroup(phoneNumber)
    }

    LaunchedEffect(groupData.members) {
        fetchGroupMembersChunked(
            memberIds = groupData.members,
            updateLoadingStatus = {},
            db = db,
            updateGroupMembers = {
                groupMembers = it
            }
        )
    }

    LoadingScreenWrapper(
        isLoading
    ) {
        Scaffold(
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState) { snackbarData ->
                    Snackbar(
                        modifier = Modifier.fillMaxWidth(0.9f),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        Text(
                            text = snackbarData.visuals.message,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        focusManager.clearFocus()
                    }
            ) {
                Column {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                "Group Settings",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.Filled.Clear,
                                    contentDescription = "Close Group Settings"
                                )
                            }
                        }
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(36.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GroupNameField(
                                groupName = groupName,
                                originalGroupName = groupData.groupName,
                                onGroupNameChange = { groupName = it },
                                isEditing = isEditingGroupName,
                                onSave = { updateGroupName() }
                            )

                            IconButton(
                                onClick = { isDeletingGroup = true }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "Delete Group"
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            AddMemberButton(
                                onClick = {
                                    contactPickerLauncher.launch(
                                        Intent(
                                            Intent.ACTION_PICK,
                                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI
                                        )
                                    )
                                }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            GroupMemberList(
                                groupData = groupData,
                                groupMembers = groupMembers,
                                updateLoadingStatus = {
                                    isLoading = it
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        if (isDeletingGroup) {
            ConfirmAlertDialog(
                title = "Delete Group",
                text = "Are you sure you want to delete this group?",
                subText = "This action cannot be undone.",
                onConfirm = { deleteGroup(groupData.id) },
                toggleAlert = { isDeletingGroup = false }
            )
        }
    }
}
