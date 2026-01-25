package com.settle.tracker.utils

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class Permissions(
    private val activity: ComponentActivity
) : DefaultLifecycleObserver {
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private val permissions = arrayOf(
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_SMS,
        Manifest.permission.POST_NOTIFICATIONS,
        Manifest.permission.READ_CONTACTS
    )

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
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

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
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
