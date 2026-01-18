package com.settle.tracker.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.settle.tracker.GoogleAuthClient
import com.settle.tracker.components.LoadingScreenWrapper
import com.settle.tracker.components.account.ProfileHeader
import com.settle.tracker.components.account.UpiIdField
import com.settle.tracker.scheme.UserScheme
import kotlinx.coroutines.launch

@Composable
fun AccountScreen(
    googleAuthClient: GoogleAuthClient,
    onLogoutSuccess: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val db = Firebase.firestore

    var user by remember { mutableStateOf(googleAuthClient.getSignedInUser()) }
    var isLoading by remember { mutableStateOf(false) }
    var isFetchingUser by remember { mutableStateOf(false) }
    var isEditingUser by remember { mutableStateOf(false) }

    var upiId by remember { mutableStateOf(TextFieldValue("")) }
    var upiIdSynced by remember { mutableStateOf("") }

    val focusManager = LocalFocusManager.current
    val upiIdFocus = remember { FocusRequester() }

    val updateUser = {
        isEditingUser = true

        try {
            val userRef = db.collection("users").document(user!!.uid)
            val updates = mapOf(
                "upiId" to upiId.text,
            )

            userRef.update(updates).addOnSuccessListener {
                upiIdSynced = upiId.text
                isEditingUser = false
            }.addOnFailureListener { e ->
                Log.e("Firestore", "${e.message}")
                isEditingUser = false
            }
        } catch (e: Exception) {
            Log.e("Firestore", "${e.message}")
            isEditingUser = false
        }
    }

    LaunchedEffect(user) {
        isFetchingUser = true

        if (user != null) {
            db.collection("users").document(user!!.uid).get().addOnSuccessListener { document ->
                if (document.exists()) {
                    val user = document.toObject(UserScheme::class.java)

                    user?.let {
                        upiId = TextFieldValue(it.upiId)
                        upiIdSynced = it.upiId
                    }
                }

                isFetchingUser = false
            }.addOnFailureListener { e ->
                isFetchingUser = false
                Log.e("Firestore", "Fetching user details failed: ${e.message}")
            }
        }
    }

    LoadingScreenWrapper(
        isFetchingUser || isLoading,
        message = when {
            isFetchingUser -> "Fetching User Details..."
            isLoading -> "Signing out..."
            else -> "Loading..."
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 20.dp, horizontal = 16.dp)
                .padding(top = 30.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    focusManager.clearFocus()

                    if (upiId.text != upiIdSynced) {
                        upiId = TextFieldValue(
                            text = upiIdSynced,
                            selection = TextRange(upiIdSynced.length)
                        )
                    }
                },
            contentAlignment = Alignment.CenterStart
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(40.dp),
            ) {
                ProfileHeader(user = user)

                UpiIdField(
                    upiId = upiId,
                    upiIdSynced = upiIdSynced,
                    isEditing = isEditingUser,
                    onUpiIdChange = { upiId = it },
                    onSave = { updateUser() },
                    focusRequester = upiIdFocus
                )

                Spacer(modifier = Modifier.weight(1f))

                TextButton(
                    onClick = {
                        isLoading = true

                        scope.launch {
                            val logoutSuccess = googleAuthClient.signOut()

                            isLoading = false

                            if (logoutSuccess) {
                                onLogoutSuccess()
                            } else {
                                Toast.makeText(context, "Sign out failed", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                    },
                    enabled = !isLoading && !isEditingUser,
                    contentPadding = PaddingValues(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.secondary,
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Logout Icon",
                        modifier = Modifier.size(20.dp),
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        "Sign Out",
                        style = MaterialTheme.typography.labelLarge,
                        letterSpacing = 1.sp,
                    )
                }
            }
        }
    }
}
