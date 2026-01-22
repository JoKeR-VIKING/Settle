package com.settle.tracker.utils

import android.app.PendingIntent
import android.content.Intent
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.settle.tracker.MainActivity
import com.settle.tracker.sms.sendNotification

fun saveTokenToFirestore(token: String) {
    val currentUser = Firebase.auth.currentUser ?: return
    val db = Firebase.firestore

    db
        .collection("users")
        .document(currentUser.uid)
        .set(
            mapOf(
                "fcmToken" to token,
                "updatedAt" to System.currentTimeMillis()
            ),
            SetOptions.merge()
        )
}

class MyFirebaseMessagingService: FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        saveTokenToFirestore(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.data["title"] ?: "New notification"
        val body = message.data["body"] ?: ""
        val category = message.data["category"] ?: "MISC"

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("destination", "groups")
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        sendNotification(
            context = this,
            title = title,
            text = body,
            category = category,
            pendingIntent = pendingIntent,
        )
    }
}
