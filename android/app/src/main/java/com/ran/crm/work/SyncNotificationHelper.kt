package com.ran.crm.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
import com.ran.crm.R

/**
 * Helper for creating the foreground notification required by [SyncWorker]
 * so that Android doesn't kill sync during Doze / App Standby.
 */
object SyncNotificationHelper {

    const val CHANNEL_ID = "crm_sync_channel"
    private const val CHANNEL_NAME = "CRM Sync"
    private const val NOTIFICATION_ID = 1001

    /** Must be called once before any foreground work (e.g. in Application.onCreate). */
    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW   // silent, no sound/vibration
            ).apply {
                description = "Background data synchronisation"
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    /** Returns a [ForegroundInfo] that keeps the worker alive in the foreground. */
    fun createForegroundInfo(context: Context): ForegroundInfo {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.logo)
            .setContentTitle("RAN CRM")
            .setContentText("Syncing contacts & calls…")
            .setOngoing(true)
            .setSilent(true)
            .build()

        return ForegroundInfo(NOTIFICATION_ID, notification)
    }
}
