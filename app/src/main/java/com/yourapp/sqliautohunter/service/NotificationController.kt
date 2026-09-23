package com.yourapp.sqliautohunter.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.yourapp.sqliautohunter.MainActivity
import com.yourapp.sqliautohunter.R
import com.yourapp.sqliautohunter.util.Constants

object NotificationController {

    private const val CHANNEL_ID = Constants.NOTIFICATION_CHANNEL_ID
    private const val CHANNEL_NAME = Constants.NOTIFICATION_CHANNEL_NAME

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "SQLi Hunter notifications"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_SECRET
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun getServiceNotification(context: Context, title: String, text: String): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(false)
            .setOngoing(true)
            .build()
    }

    fun createForegroundNotification(context: Context, title: String, text: String): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Create pause action
        val pauseIntent = Intent(context, ScanForegroundService::class.java).apply {
            action = Constants.SERVICE_ACTION_PAUSE
        }
        val pausePendingIntent = PendingIntent.getService(
            context,
            1,
            pauseIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Create stop action
        val stopIntent = Intent(context, ScanForegroundService::class.java).apply {
            action = Constants.SERVICE_ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            context,
            2,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(R.drawable.ic_notification, "Pause", pausePendingIntent)
            .addAction(R.drawable.ic_notification, "Stop", stopPendingIntent)
            .build()
    }

    fun showNotification(
        context: Context,
        id: Int,
        title: String,
        text: String,
        autoCancel: Boolean = true
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = getServiceNotification(context, title, text)
        
        if (autoCancel) {
            notification.flags = notification.flags or Notification.FLAG_AUTO_CANCEL
        }
        
        notificationManager.notify(id, notification)
    }

    fun updateScanProgressNotification(
        context: Context,
        tested: Int,
        vulnerable: Int,
        queueRemaining: Int
    ) {
        val text = "Tested: $tested | Vulnerable: $vulnerable | Queue: $queueRemaining"
        showNotification(context, Constants.NOTIFICATION_ID, "SQLi Hunter", text, false)
    }

    fun showScanCompleteNotification(context: Context, results: Int) {
        val text = "Scan complete! Found $results vulnerabilities"
        showNotification(context, 2, "SQLi Hunter", text, true)
    }

    fun showScanErrorNotification(context: Context, error: String) {
        showNotification(context, 3, "SQLi Hunter Error", error, true)
    }

    fun cancelAllNotifications(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
    }

    fun cancelNotification(context: Context, id: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(id)
    }
}
