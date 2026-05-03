package com.settle.tracker.screens

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.google.firebase.Firebase
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.firestore
import com.settle.tracker.components.MultiDigitTextField
import java.util.concurrent.TimeUnit

@Composable
fun PhoneVerificationScreen(
    onPhoneVerified: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val activity = LocalContext.current as Activity
    val context = LocalContext.current

    val db = Firebase.firestore

    var phoneNumber by remember { mutableStateOf(TextFieldValue("")) }
    var verificationId by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    fun sendOTP(
        activity: Activity,
        phoneNumber: String
    ) {
        isLoading = true

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                FirebaseAuth.getInstance()
                    .currentUser
                    ?.linkWithCredential(credential)
                    ?.addOnSuccessListener {
                        isLoading = false
                    }
            }

            override fun onVerificationFailed(p0: FirebaseException) {
                isLoading = false
                Toast.makeText(
                    context,
                    "SMS Verification Failed",
                    Toast.LENGTH_SHORT
                ).show()
            }

            override fun onCodeSent(
                id: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                verificationId = id
                isLoading = false
            }
        }

        val options = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
            .setPhoneNumber("+91$phoneNumber")
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyOtp(
        verificationId: String,
        otp: String
    ) {
        isLoading = true

        val credential = PhoneAuthProvider.getCredential(verificationId, otp)
        val user = FirebaseAuth.getInstance().currentUser
            ?: return

        user.linkWithCredential(credential)
            .addOnSuccessListener {
                db
                    .collection("users")
                    .document(user.uid)
                    .update("phoneNumber", user.phoneNumber)
                    .addOnSuccessListener {
                        onPhoneVerified()
                        isLoading = false
                    }
                    .addOnFailureListener {
                        isLoading = false
                    }
            }
            .addOnFailureListener {
                isLoading = false
                Toast.makeText(
                    context,
                    "Incorrect OTP entered",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .padding(vertical = 24.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    focusManager.clearFocus()
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(0.95f),
                text = "Verify your phone number",
                style = MaterialTheme.typography.bodyLarge
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(0.95f),
                textStyle = MaterialTheme.typography.labelLarge,
                shape = RoundedCornerShape(15),
                value = phoneNumber,
                onValueChange = { input ->
                    val normalized = input
                        .text
                        .trim()
                        .replace("\\s".toRegex(), "")
                        .let {
                            when {
                                it.startsWith("+91") -> it.drop(3)
                                it.startsWith("91") && it.length > 10 -> it.drop(2)
                                else -> it
                            }
                        }
                        .filter { it.isDigit() }
                        .take(10)

                    phoneNumber =
                        TextFieldValue(normalized, selection = TextRange(normalized.length))
                },
                label = {
                    Text("Phone Number", style = MaterialTheme.typography.labelMedium)
                },
                supportingText = {
                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(
                            onClick = {
                                verificationId = ""
                                phoneNumber = TextFieldValue("")
                            },
                            modifier = Modifier.align(Alignment.CenterEnd),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            enabled = verificationId.isNotBlank()
                        ) {
                            Text(
                                "Change phone number",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Done
                )
            )

            if (verificationId.isNotBlank()) {
                MultiDigitTextField(
                    numberOfDigits = 6,
                    otp,
                    onValueChange = { otp = it },
                    isVisible = verificationId.isNotBlank(),
                    helperText = "Enter OTP"
                )
            }

            Button(
                onClick = {
                    if (verificationId.isBlank()) {
                        sendOTP(
                            activity = activity,
                            phoneNumber = phoneNumber.text
                        )
                    } else {
                        verifyOtp(
                            verificationId,
                            otp
                        )
                    }
                },
                shape = RoundedCornerShape(25),
                enabled = !isLoading && if (verificationId.isBlank()) phoneNumber.text.length == 10 else otp.length == 6
            ) {
                Text(
                    text = if (verificationId.isBlank()) "Send OTP" else "Verify OTP",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}
