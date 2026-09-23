package com.yourapp.sqliautohunter.engine.crash

import android.util.Log
import com.yourapp.sqliautohunter.data.repository.CrashLogRepository
import com.yourapp.sqliautohunter.data.local.database.entity.CrashLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.PrintWriter
import java.io.StringWriter

class CrashLogManager(
    private val crashLogRepository: CrashLogRepository,
    private val scope: CoroutineScope
) {

    private val moduleName = "CrashLogManager"
    private var isInitialized = false

    fun initialize() {
        if (isInitialized) return
        
        // Set up global exception handler
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            handleUncaughtException(thread, throwable)
        }
        
        isInitialized = true
        Log.d(moduleName, "CrashLogManager initialized")
    }

    private fun handleUncaughtException(thread: Thread, throwable: Throwable) {
        scope.launch(Dispatchers.IO) {
            logException(thread, throwable, "Uncaught Exception")
        }
    }

    suspend fun logException(
        throwable: Throwable,
        module: String,
        context: String = "",
        url: String? = null,
        severity: String = "Error"
    ) {
        try {
            val stackTrace = getStackTraceAsString(throwable)
            val errorMessage = throwable.message ?: "No error message"
            
            val logEntity = CrashLogEntity(
                timestamp = System.currentTimeMillis(),
                moduleName = module,
                errorMessage = "$errorMessage - $context",
                stackTrace = stackTrace,
                urlBeingProcessed = url,
                severity = severity
            )
            
            crashLogRepository.saveLog(logEntity)
            
            Log.e(module, "Exception logged: $errorMessage", throwable)
            
        } catch (e: Exception) {
            Log.e(moduleName, "Failed to log exception", e)
        }
    }

    suspend fun logException(
        throwable: Throwable,
        thread: Thread,
        module: String = "Global"
    ) {
        logException(throwable, module, "Thread: ${thread.name}")
    }

    suspend fun logWarning(
        message: String,
        module: String,
        url: String? = null
    ) {
        try {
            val logEntity = CrashLogEntity(
                timestamp = System.currentTimeMillis(),
                moduleName = module,
                errorMessage = message,
                stackTrace = "",
                urlBeingProcessed = url,
                severity = "Warning"
            )
            
            crashLogRepository.saveLog(logEntity)
            Log.w(module, message)
            
        } catch (e: Exception) {
            Log.e(moduleName, "Failed to log warning", e)
        }
    }

    suspend fun logFatal(
        message: String,
        module: String,
        url: String? = null,
        throwable: Throwable? = null
    ) {
        try {
            val stackTrace = throwable?.let { getStackTraceAsString(it) } ?: ""
            val errorMessage = throwable?.message ?: message
            
            val logEntity = CrashLogEntity(
                timestamp = System.currentTimeMillis(),
                moduleName = module,
                errorMessage = errorMessage,
                stackTrace = stackTrace,
                urlBeingProcessed = url,
                severity = "Fatal"
            )
            
            crashLogRepository.saveLog(logEntity)
            Log.wtf(module, message, throwable)
            
        } catch (e: Exception) {
            Log.e(moduleName, "Failed to log fatal error", e)
        }
    }

    private fun getStackTraceAsString(throwable: Throwable): String {
        val stringWriter = StringWriter()
        val printWriter = PrintWriter(stringWriter)
        throwable.printStackTrace(printWriter)
        printWriter.close()
        return stringWriter.toString()
    }

    fun logToConsole(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
    }

    suspend fun getRecentLogs(limit: Int = 100): List<CrashLogEntity> {
        return crashLogRepository.getRecentLogs(limit)
    }

    suspend fun clearLogs() {
        crashLogRepository.clearLogs()
    }

    fun getLogCount(): Int {
        // This would need to be a suspend function or use Flow
        // For simplicity, we'll use a cached value or return 0
        return 0
    }
}
