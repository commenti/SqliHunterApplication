package com.yourapp.sqliautohunter.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.yourapp.sqliautohunter.MainActivity
import com.yourapp.sqliautohunter.R
import com.yourapp.sqliautohunter.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ScanForegroundService : Service() {

    private val binder = LocalBinder()
    private lateinit var notificationManager: NotificationManager
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private var isServiceRunning = false
    private var currentNotification: Notification? = null

    inner class LocalBinder : Binder() {
        fun getService(): ScanForegroundService = this@ScanForegroundService
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        isServiceRunning = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let { handleIntent(it) }
        
        // Start as foreground service
        startForeground()
        
        return START_STICKY
    }

    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            Constants.SERVICE_ACTION_START -> startScan()
            Constants.SERVICE_ACTION_PAUSE -> pauseScan()
            Constants.SERVICE_ACTION_RESUME -> resumeScan()
            Constants.SERVICE_ACTION_STOP -> stopScan()
        }
    }

    private fun startForeground() {
        val notification = createNotification()
        // Android 14+ (enforced on 15) requires the foreground service type
        // at promotion time. ServiceCompat ignores the type below API 29,
        // where the manifest-declared type does not exist yet.
        val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        } else {
            0
        }
        ServiceCompat.startForeground(
            this,
            Constants.NOTIFICATION_ID,
            notification,
            serviceType
        )
        currentNotification = notification
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_ID,
                Constants.NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "SQLi Hunter Service"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_SECRET
            }
            
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent: PendingIntent = Intent(this, MainActivity::class.java).let { intent ->
            PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        // Create pause action
        val pauseIntent = Intent(this, ScanForegroundService::class.java).apply {
            action = Constants.SERVICE_ACTION_PAUSE
        }
        val pausePendingIntent = PendingIntent.getService(
            this,
            1,
            pauseIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Create stop action
        val stopIntent = Intent(this, ScanForegroundService::class.java).apply {
            action = Constants.SERVICE_ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            2,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("SQLi Hunter")
            .setContentText("Service is running")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(R.drawable.ic_notification, "Pause", pausePendingIntent)
            .addAction(R.drawable.ic_notification, "Stop", stopPendingIntent)
            .build()
    }

    private fun startScan() {
        scope.launch {
            // This would be handled by the worker pool
            // For now, just update notification
            updateNotification("Scanning...", 0, 0, 0)
        }
    }

    private fun pauseScan() {
        scope.launch {
            updateNotification("Paused", 0, 0, 0)
        }
    }

    private fun resumeScan() {
        scope.launch {
            updateNotification("Scanning...", 0, 0, 0)
        }
    }

    private fun stopScan() {
        scope.launch {
            stopSelf()
        }
    }

    fun updateNotification(
        status: String,
        tested: Int,
        vulnerable: Int,
        queueRemaining: Int
    ) {
        val text = "$status | Tested: $tested | Vulnerable: $vulnerable | Queue: $queueRemaining"
        
        val pauseIntent = Intent(this, ScanForegroundService::class.java).apply {
            action = Constants.SERVICE_ACTION_PAUSE
        }
        val pausePendingIntent = PendingIntent.getService(
            this,
            1,
            pauseIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, ScanForegroundService::class.java).apply {
            action = Constants.SERVICE_ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            2,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("SQLi Hunter")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(R.drawable.ic_notification, "Pause", pausePendingIntent)
            .addAction(R.drawable.ic_notification, "Stop", stopPendingIntent)
            .build()

        currentNotification = notification
        notificationManager.notify(Constants.NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        scope.cancel()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    }

    fun isRunning(): Boolean = isServiceRunning
}
