package com.ran.crm.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner

class PermissionsManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {

    private var permissionLauncher: ActivityResultLauncher<Array<String>>? = null

    // Permission request callbacks
    var onContactsPermissionGranted: (() -> Unit)? = null
    var onContactsPermissionDenied: (() -> Unit)? = null
    var onCallLogPermissionGranted: (() -> Unit)? = null
    var onCallLogPermissionDenied: (() -> Unit)? = null

    init {
        if (context is ComponentActivity) {
            permissionLauncher = context.registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                handlePermissionResults(permissions)
            }
        }
    }

    /**
     * Check if contacts permission is granted
     */
    fun hasContactsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check if call log permission is granted
     */
    fun hasCallLogPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Request contacts permission
     */
    fun requestContactsPermission() {
        permissionLauncher?.launch(arrayOf(Manifest.permission.READ_CONTACTS))
    }

    /**
     * Request call log permission
     */
    fun requestCallLogPermission() {
        permissionLauncher?.launch(arrayOf(Manifest.permission.READ_CALL_LOG))
    }

    /**
     * Request both permissions
     */
    fun requestAllPermissions() {
        val permissions = mutableListOf<String>()
        if (!hasContactsPermission()) {
            permissions.add(Manifest.permission.READ_CONTACTS)
        }
        if (!hasCallLogPermission()) {
            permissions.add(Manifest.permission.READ_CALL_LOG)
        }

        if (permissions.isNotEmpty()) {
            permissionLauncher?.launch(permissions.toTypedArray())
        }
    }

    /**
     * Open app settings for manual permission management
     */
    fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        context.startActivity(intent)
    }

    /**
     * Check if permissions are permanently denied
     */
    fun shouldShowRationale(permission: String): Boolean {
        return if (context is ComponentActivity) {
            context.shouldShowRequestPermissionRationale(permission)
        } else {
            false
        }
    }

    private fun handlePermissionResults(permissions: Map<String, Boolean>) {
        permissions.forEach { (permission, granted) ->
            when (permission) {
                Manifest.permission.READ_CONTACTS -> {
                    if (granted) {
                        onContactsPermissionGranted?.invoke()
                    } else {
                        onContactsPermissionDenied?.invoke()
                    }
                }
                Manifest.permission.READ_CALL_LOG -> {
                    if (granted) {
                        onCallLogPermissionGranted?.invoke()
                    } else {
                        onCallLogPermissionDenied?.invoke()
                    }
                }
            }
        }
    }

    /**
     * Get user-friendly permission names
     */
    fun getPermissionDisplayName(permission: String): String {
        return when (permission) {
            Manifest.permission.READ_CONTACTS -> "Contacts"
            Manifest.permission.READ_CALL_LOG -> "Call Logs"
            else -> permission
        }
    }

    /**
     * Check if all required permissions are granted
     */
    fun hasAllRequiredPermissions(): Boolean {
        return hasContactsPermission() && hasCallLogPermission()
    }
}
