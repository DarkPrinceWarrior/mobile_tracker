package com.example.mobile_tracker.data.ble

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object BlePermissionManager {

    /** Список runtime-разрешений, необходимых для BLE. */
    fun requiredPermissions(): List<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
            )
        } else {
            listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
            )
        }

    /** true — все BLE-разрешения выданы. */
    fun hasPermissions(context: Context): Boolean =
        requiredPermissions().all { perm ->
            ContextCompat.checkSelfPermission(
                context, perm,
            ) == PackageManager.PERMISSION_GRANTED
        }
}
