package com.settle.tracker.utils

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await

const val PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=com.settle.tracker"

sealed class UpdateStatus {
    object UpToDate : UpdateStatus()
    object OptionalUpdate : UpdateStatus()
    object ForceUpdate : UpdateStatus()
}

suspend fun checkForUpdate(currentVersionCode: Int): UpdateStatus {
    return try {
        val doc = Firebase.firestore
            .collection("app_config")
            .document("update")
            .get()
            .await()

        val minVersionCode = doc.getLong("minVersionCode")?.toInt() ?: 0
        val latestVersionCode = doc.getLong("latestVersionCode")?.toInt() ?: 0

        when {
            currentVersionCode < minVersionCode -> UpdateStatus.ForceUpdate
            currentVersionCode < latestVersionCode -> UpdateStatus.OptionalUpdate
            else -> UpdateStatus.UpToDate
        }
    } catch (_: Exception) {
        UpdateStatus.UpToDate
    }
}
