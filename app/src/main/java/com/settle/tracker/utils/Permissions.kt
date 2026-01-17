package com.settle.tracker.utils

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class Permissions(
    private val activity: ComponentActivity
) : DefaultLifecycleObserver {
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

    private val permissions = arrayOf(
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_SMS,
        Manifest.permission.POST_NOTIFICATIONS,
        Manifest.permission.READ_CONTACTS
    )

    override fun onCreate(owner: LifecycleOwner) {
        permissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->
            result.forEach { (permission, isGranted) ->
                if (isGranted) onPermissionGranted(permission)
            }
        }

        askPermissions()
    }

    fun askPermissions() {
        val permissionsToRequest = permissions.filter {
            ContextCompat
                .checkSelfPermission(
                    activity,
                    it
                ) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            permissions.forEach { onPermissionGranted(it) }
        }
    }
}

private fun onPermissionGranted(permission: String) {
    Log.d("Permission_Active", permission)
}
