package com.settle.tracker.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.settle.tracker.GoogleAuthClient
import com.settle.tracker.components.LoadingScreenWrapper
import com.settle.tracker.components.account.ProfileHeader
import com.settle.tracker.components.account.UpiIdField
import com.settle.tracker.components.common.ProfileSkeleton
import com.settle.tracker.components.common.ScreenHeader
import com.settle.tracker.scheme.UserScheme
import kotlinx.coroutines.launch

@Composable
fun AccountScreen(
    googleAuthClient: GoogleAuthClient,
    darkThemeEnabled: Boolean,
    onThemeToggle: (Boolean) -> Unit,
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
        isLoading = isLoading,
        message = when {
            isLoading -> "Signing out..."
            else -> "Loading..."
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
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
                }
        ) {
            ScreenHeader(
                title = "Profile",
                subtitle = "Manage payment details, appearance, and the account tied to Settle."
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                if (isFetchingUser) {
                    ProfileSkeleton()
                } else {
                    ProfileHeader(user = user)

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Dark mode",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Switch between brighter daytime and quieter nighttime styling.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Switch(
                                checked = darkThemeEnabled,
                                onCheckedChange = onThemeToggle
                            )
                        }
                    }

                    UpiIdField(
                        upiId = upiId,
                        upiIdSynced = upiIdSynced,
                        isEditing = isEditingUser,
                        onUpiIdChange = { upiId = it },
                        onSave = { updateUser() },
                        focusRequester = upiIdFocus
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                TextButton(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
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
                    contentPadding = PaddingValues(16.dp),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Logout Icon",
                        modifier = Modifier.size(20.dp),
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        "Sign Out",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                }
            }
        }
    }
}
