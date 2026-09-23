package com.settle.tracker.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.settle.tracker.BuildConfig
import com.settle.tracker.GoogleAuthClient
import com.settle.tracker.components.LoadingScreenWrapper
import com.settle.tracker.components.account.ProfileHeader
import com.settle.tracker.components.account.UpiIdField
import com.settle.tracker.scheme.UserScheme
import com.settle.tracker.ui.animations.bounceClickable
import com.settle.tracker.utils.LocalThemeState
import com.settle.tracker.utils.SettleLinks
import com.settle.tracker.utils.SettlePrefs
import com.settle.tracker.utils.ThemeMode
import com.settle.tracker.utils.isAdminUser
import com.settle.tracker.utils.openUrl
import kotlinx.coroutines.launch

@Composable
fun AccountScreen(
    googleAuthClient: GoogleAuthClient,
    onLogoutSuccess: () -> Unit,
    onReportIssue: () -> Unit = {},
    onOpenIssueReports: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { SettlePrefs(context.applicationContext) }
    val themeState = LocalThemeState.current

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
            val updates = mapOf("upiId" to upiId.text)
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
                    val fetched = document.toObject(UserScheme::class.java)
                    fetched?.let {
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
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp)
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(18.dp),
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

                SectionLabel("Preferences")

                ThemeSelectorCard(
                    currentMode = themeState.mode.value,
                    onSelect = { themeState.set(it) }
                )

                SectionLabel("About")

                ActionRow(
                    icon = Icons.Filled.PrivacyTip,
                    title = "Privacy Policy",
                    subtitle = "How we handle your data",
                    onClick = { context.openUrl(SettleLinks.PRIVACY_POLICY) }
                )

                ActionRow(
                    icon = Icons.Filled.School,
                    title = "Replay Tour",
                    subtitle = "Re-show the quick intro tutorials",
                    onClick = {
                        // Reset first-run flags so coachmarks appear again
                        prefs.resetTours()
                        db.collection("users").document(user!!.uid)
                            .update("tourTaken", false)
                            .addOnFailureListener { Log.e("Firestore", "Failed to reset tour: ${it.message}") }

                        Toast.makeText(
                            context,
                            "Tour reset! Visit Expenses, Groups or Analytics to see it again.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )

                ActionRow(
                    icon = Icons.Filled.BugReport,
                    title = "Report Issue",
                    subtitle = "Something not working? Let us know",
                    onClick = onReportIssue
                )

                if (isAdminUser(user?.email)) {
                    SectionLabel("Admin")

                    ActionRow(
                        icon = Icons.Filled.AdminPanelSettings,
                        title = "Issue Reports",
                        subtitle = "View reports submitted by users",
                        onClick = onOpenIssueReports
                    )
                }

                Spacer(Modifier.height(8.dp))

                SignOutButton(
                    enabled = !isLoading && !isEditingUser,
                    onClick = {
                        isLoading = true
                        scope.launch {
                            val logoutSuccess = googleAuthClient.signOut()
                            isLoading = false
                            if (logoutSuccess) onLogoutSuccess()
                            else Toast.makeText(context, "Sign out failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                )

                Text(
                    text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
private fun ThemeSelectorCard(
    currentMode: ThemeMode,
    onSelect: (ThemeMode) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                Icons.Filled.Brightness6,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text("Appearance", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(
                Triple(ThemeMode.SYSTEM, "System", Icons.Filled.Brightness6),
                Triple(ThemeMode.LIGHT, "Light", Icons.Filled.LightMode),
                Triple(ThemeMode.DARK, "Dark", Icons.Filled.DarkMode),
            ).forEach { (mode, label, icon) ->
                val active = currentMode == mode
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(11.dp))
                        .background(
                            if (active) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable { onSelect(mode) }
                        .padding(vertical = 10.dp, horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (active) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(
                        label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (active) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .bounceClickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SignOutButton(
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .bounceClickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.AutoMirrored.Filled.Logout,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.size(10.dp))
        Text(
            "Sign Out",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.error
        )
    }
}
