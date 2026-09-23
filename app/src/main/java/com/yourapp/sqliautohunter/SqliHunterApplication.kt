package com.yourapp.sqliautohunter

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.yourapp.sqliautohunter.engine.crash.GlobalExceptionHandler
import com.yourapp.sqliautohunter.util.PermissionHelper
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SqliHunterApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var globalExceptionHandler: GlobalExceptionHandler

    @Inject
    lateinit var permissionHelper: PermissionHelper

    override fun onCreate() {
        super.onCreate()

        // GlobalExceptionHandler installs itself in init; no initialize() call needed.
        //
        // Startup discipline (Android 12+, enforced on Android 15):
        // - Do NOT start the foreground service here. startForegroundService()
        //   from Application.onCreate throws ForegroundServiceStartNotAllowedException
        //   whenever the process starts while the app is in the background
        //   (boot, WorkManager reschedule, service restart). The service is
        //   started only from explicit user scan actions and the WorkManager
        //   watchdog, both of which run in a valid foreground context.
        // - Do NOT fire ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS here either:
        //   it yanks the user to system settings on every cold start and can
        //   throw ActivityNotFoundException on devices without a handler.
        //   MainActivity prompts for it once, guarded, from the foreground.
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    fun requestPermissions() {
        // This would be called from an Activity
    }
}
