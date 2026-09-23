package com.yourapp.sqliautohunter

import android.app.Application
import android.content.Intent
import android.os.Build
import com.yourapp.sqliautohunter.di.AppModule
import com.yourapp.sqliautohunter.engine.crash.GlobalExceptionHandler
import com.yourapp.sqliautohunter.service.ScanForegroundService
import com.yourapp.sqliautohunter.util.PermissionHelper
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SqliHunterApplication : Application() {

    @Inject
    lateinit var globalExceptionHandler: GlobalExceptionHandler

    @Inject
    lateinit var permissionHelper: PermissionHelper

    override fun onCreate() {
        super.onCreate()
        
        // Initialize global exception handler
        globalExceptionHandler.initialize()
        
        // Request battery optimization exemption
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            permissionHelper.requestBatteryOptimizationExemption(this)
        }
        
        // Start foreground service if needed
        startForegroundService()
    }

    private fun startForegroundService() {
        val serviceIntent = Intent(this, ScanForegroundService::class.java).apply {
            action = com.yourapp.sqliautohunter.util.Constants.SERVICE_ACTION_START
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    fun requestPermissions() {
        // This would be called from an Activity
    }
}
