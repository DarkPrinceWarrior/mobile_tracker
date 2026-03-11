package com.example.mobile_tracker.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.mobile_tracker.R
import com.example.mobile_tracker.data.local.datastore.UserPreferencesManager
import kotlinx.coroutines.flow.first

class OperatorNotificationManager(
    private val context: Context,
    private val preferencesManager: UserPreferencesManager,
) {

    suspend fun notifyPendingPackets(count: Int) {
        if (count <= 0 || !canNotify()) return
        notify(
            id = 1201,
            channelId = CHANNEL_SYNC,
            title = context.getString(R.string.notifications_pending_packets_title),
            text = context.getString(R.string.notifications_pending_packets_body, count),
        )
    }

    suspend fun notifyPacketUploadError(
        deviceId: String,
        error: String?,
    ) {
        if (!canNotify()) return
        notify(
            id = 1202 + deviceId.hashCode(),
            channelId = CHANNEL_ERRORS,
            title = context.getString(R.string.notifications_upload_error_title),
            text = error?.takeIf { it.isNotBlank() }
                ?: context.getString(R.string.notifications_upload_error_body, deviceId),
        )
    }

    suspend fun notifyBindingConflict(deviceId: String) {
        if (!canNotify()) return
        notify(
            id = 1203 + deviceId.hashCode(),
            channelId = CHANNEL_ERRORS,
            title = context.getString(R.string.notifications_binding_conflict_title),
            text = context.getString(R.string.notifications_binding_conflict_body, deviceId),
        )
    }

    suspend fun notifySyncCompleted(count: Int) {
        if (count <= 0 || !canNotify()) return
        notify(
            id = 1204,
            channelId = CHANNEL_SYNC,
            title = context.getString(R.string.notifications_sync_complete_title),
            text = context.getString(R.string.notifications_sync_complete_body, count),
        )
    }

    suspend fun notifyReferenceSyncFailed() {
        if (!canNotify()) return
        notify(
            id = 1205,
            channelId = CHANNEL_ERRORS,
            title = context.getString(R.string.notifications_reference_sync_error_title),
            text = context.getString(R.string.notifications_reference_sync_error_body),
        )
    }

    fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val syncChannel = NotificationChannel(
            CHANNEL_SYNC,
            context.getString(R.string.notifications_channel_sync_title),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notifications_channel_sync_desc)
        }

        val errorChannel = NotificationChannel(
            CHANNEL_ERRORS,
            context.getString(R.string.notifications_channel_errors_title),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notifications_channel_errors_desc)
        }

        manager.createNotificationChannel(syncChannel)
        manager.createNotificationChannel(errorChannel)
    }

    private suspend fun canNotify(): Boolean {
        val prefs = preferencesManager.userPreferences.first()
        if (!prefs.notificationsEnabled) return false
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    private fun notify(
        id: Int,
        channelId: String,
        title: String,
        text: String,
    ) {
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(id, notification)
    }

    companion object {
        const val CHANNEL_SYNC = "operator_sync"
        const val CHANNEL_ERRORS = "operator_errors"
    }
}
