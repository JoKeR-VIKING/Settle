package com.settle.tracker.components.groups

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.firestore
import com.settle.tracker.components.ConfirmAlertDialog
import com.settle.tracker.scheme.GroupScheme
import com.settle.tracker.scheme.UserScheme

@Composable
fun GroupMemberList(
    groupData: GroupScheme,
    updateLoadingStatus: (Boolean) -> Unit,
    onClick: ((UserScheme) -> Unit)? = null,
    modifier: Modifier
) {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    var removingMemberId by remember { mutableStateOf("") }
    var groupMembers by remember { mutableStateOf(emptyList<UserScheme>()) }

    val db = Firebase.firestore

    fun onDeleteMember(userId: String) {
        updateLoadingStatus(true)
        removingMemberId = ""

        val updatedGroupMembers = groupData.members.toMutableList()
        updatedGroupMembers.remove(userId)

        db
            .collection("groups")
            .document(groupData.id)
            .update("members", updatedGroupMembers)
            .addOnSuccessListener {
                updateLoadingStatus(false)
            }
            .addOnFailureListener {
                Log.e("Firestore", "${it.message}")
                updateLoadingStatus(false)
            }
    }

    fun <T> List<T>.chunkedSafe(size: Int = 10) = this.chunked(size)

    fun fetchGroupMembersChunked(
        memberIds: List<String>
    ) {
        val result = mutableListOf<UserScheme>()
        updateLoadingStatus(true)

        memberIds.chunkedSafe().forEach { chunk ->
            db
                .collection("users")
                .whereIn(FieldPath.documentId(), chunk)
                .get()
                .addOnSuccessListener {
                    result.addAll(it.toObjects(UserScheme::class.java))
                    if (result.size >= memberIds.size) {
                        groupMembers = result
                    }
                    updateLoadingStatus(false)
                }
                .addOnFailureListener {
                    updateLoadingStatus(false)
                }
        }
    }

    LaunchedEffect(groupData.members) {
        fetchGroupMembersChunked(groupData.members)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        groupMembers.forEach { member ->
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            onClick = {
                                if (onClick != null) onClick(member)
                            }
                        )
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(25.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = member.photoUrl,
                            contentDescription = "Profile Picture",
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop,
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                member.name,
                                style = MaterialTheme.typography.labelLarge
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    member.phoneNumber,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (
                                    onClick == null &&
                                    groupData.createdBy == member.id
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                MaterialTheme.colorScheme.primary,
                                                RoundedCornerShape(50)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Admin",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (
                        onClick == null &&
                        currentUser?.uid == groupData.createdBy &&
                        currentUser?.uid != member.id
                    ) {
                        IconButton(
                            onClick = { removingMemberId = member.id }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Remove,
                                contentDescription = "Remove Member"
                            )
                        }
                    }
                }
            }
        }
    }

    if (removingMemberId.isNotBlank()) {
        ConfirmAlertDialog(
            title = "Remove Member",
            text = "Are you sure you want to remove this member?",
            subText = "This action cannot be undone.",
            onConfirm = { onDeleteMember(removingMemberId) },
            toggleAlert = { removingMemberId = "" }
        )
    }
}
