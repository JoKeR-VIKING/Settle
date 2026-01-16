package com.settle.tracker

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest

import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.ClearCredentialException

import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseUser

import kotlinx.coroutines.tasks.await

class GoogleAuthClient(private val context: Context) {
    private val auth = FirebaseAuth.getInstance()
    private val credentialManager = CredentialManager.create(context)

    suspend fun signIn(): Boolean {
        try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(context.getString(R.string.default_web_client_id))
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = context
            )

            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                try {
                    val googleIdTokenCredential =
                        GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken

                    val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                    auth.signInWithCredential(firebaseCredential).await()
                    return true
                } catch (e: Exception) {
                    Log.e("Auth", "Data parsing failed: ${e.message}")
                    return false
                }
            } else {
                Log.e("Auth", "Unexpected credential type: ${credential.javaClass.name}")
                return false
            }
        } catch (e: GetCredentialException) {
            Log.e("Auth", "Sign in failed: ${e.message}")
            return false // User cancelled or error
        } catch (e: Exception) {
            Log.e("Auth", "Firebase auth failed: ${e.message}")
            return false
        }
    }

    fun getSignedInUser(): FirebaseUser? = auth.currentUser

    suspend fun signOut(): Boolean {
        try {
            val clearRequest = ClearCredentialStateRequest()
            credentialManager.clearCredentialState(clearRequest)
            auth.signOut()

            return true
        } catch (e: ClearCredentialException) {
            Log.e("Auth", "Could not clear credentials: ${e.localizedMessage}")
            return false
        }
    }
}
