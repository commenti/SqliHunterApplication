package com.yourapp.sqliautohunter.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Restarts the scan watchdog after device boot.
 *
 * Background-start safe (Android 12+, enforced on Android 15): this only
 * enqueues WorkManager work. Never call startForegroundService() from here —
 * the app is in the background at boot and that throws
 * ForegroundServiceStartNotAllowedException. The worker promotes itself to a
 * foreground worker via getForegroundInfo() when it actually runs.
 */
class ScanBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != "android.intent.action.QUICKBOOT_POWERON"
        ) {
            return
        }

        val watchdogRequest =
            PeriodicWorkRequestBuilder<ScanWorkManagerWorker>(30, TimeUnit.MINUTES)
                .addTag(ScanWorkManagerWorker.WORKER_TAG)
                .build()

        WorkManager.getInstance(context.applicationContext)
            .enqueueUniquePeriodicWork(
                WATCHDOG_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                watchdogRequest
            )
    }

    companion object {
        const val WATCHDOG_WORK_NAME = "sqli_scan_watchdog"
    }
}
