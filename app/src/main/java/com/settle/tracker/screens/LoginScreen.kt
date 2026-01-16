package com.settle.tracker.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseUser
import com.settle.tracker.GoogleAuthClient
import com.settle.tracker.R
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    googleAuthClient: GoogleAuthClient,
    onLoginSuccess: (FirebaseUser) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "App Logo",
                    modifier = Modifier
                        .size(150.dp)
                        .clip(RoundedCornerShape(percent = 25)),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    "Settle",
                    style = MaterialTheme.typography.headlineLarge,
                    letterSpacing = 1.5.sp
                )
            }

            OutlinedButton(
                onClick = {
                    isLoading = true

                    scope.launch {
                        val loginSuccess = googleAuthClient.signIn()

                        isLoading = false

                        if (loginSuccess) {
                            googleAuthClient.getSignedInUser()?.let { user ->
                                onLoginSuccess(user)
                            }
                        } else {
                            Toast.makeText(context, "Sign in failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                enabled = !isLoading,
                shape = RoundedCornerShape(percent = 20),
                modifier = Modifier.fillMaxWidth(0.8f),
                contentPadding = PaddingValues(vertical = 14.dp),
                border = BorderStroke(width = 2.dp, color = MaterialTheme.colorScheme.secondary),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                )
            ) {
                Image(
                    painter = painterResource(id = R.drawable.google_logo),
                    contentDescription = "Google Logo",
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    "Sign in with Google",
                    style = MaterialTheme.typography.bodyLarge,
                    letterSpacing = 1.sp,
                )
            }
        }
    }
}
