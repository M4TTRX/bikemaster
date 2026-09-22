package com.velo.wear.communication

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.velo.wear.MainActivity

class WearOsDataListenerService : WearableListenerService() {

    companion object {
        private const val TAG = "WearOsDataListener"
        const val PATH_RIDE_STATUS = "/velo/status"
        const val PATH_OPEN_COMPANION = "/velo/open_companion"
        private const val CHANNEL_ID = "velo_companion_launch"
        const val NOTIFICATION_ID = 2001
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        Log.d(TAG, "onMessageReceived: path=${messageEvent.path}")
        when (messageEvent.path) {
            PATH_OPEN_COMPANION -> {
                launchCompanionApp()
            }
            PATH_RIDE_STATUS -> {
                val status = String(messageEvent.data, Charsets.UTF_8).trim()
                if (status.equals("RECORDING", ignoreCase = true)) {
                    launchCompanionApp()
                }
            }
        }
    }

    private fun launchCompanionApp() {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        try {
            startActivity(launchIntent)
            Log.d(TAG, "Direct startActivity succeeded")
        } catch (e: Throwable) {
            Log.w(TAG, "Direct startActivity failed, falling back to fullScreenIntent", e)
        }

        // Post high-priority notification with full-screen intent to ensure wake-up
        try {
            showFullScreenNotification(launchIntent)
        } catch (e: Throwable) {
            Log.w(TAG, "showFullScreenNotification failed", e)
        }
    }

    private fun showFullScreenNotification(intent: Intent) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Ride Companion",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Opens Velo companion app on watch"
        }
        notificationManager.createNotificationChannel(channel)

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Velo Ride Active")
            .setContentText("Tap to open companion")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pendingIntent, true)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
