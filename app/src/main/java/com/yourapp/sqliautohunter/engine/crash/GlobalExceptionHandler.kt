package com.yourapp.sqliautohunter.engine.crash

import android.content.Context
import android.util.Log
import com.yourapp.sqliautohunter.data.repository.CrashLogRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.PrintWriter
import java.io.StringWriter

class GlobalExceptionHandler(
    private val context: Context,
    private val crashLogRepository: CrashLogRepository
) : Thread.UncaughtExceptionHandler {

    private val defaultHandler: Thread.UncaughtExceptionHandler? = Thread.getDefaultUncaughtExceptionHandler()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val tag = "GlobalExceptionHandler"

    init {
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            // Log to crash repository
            logException(thread, throwable)
            
            // Also log to console
            Log.e(tag, "Uncaught exception in thread: ${thread.name}", throwable)
            
            // Chain to default handler if it exists
            defaultHandler?.uncaughtException(thread, throwable)
            
        } catch (e: Exception) {
            // If we can't log the exception, at least log to console
            Log.e(tag, "Failed to handle uncaught exception", e)
        }
    }

    private fun logException(thread: Thread, throwable: Throwable) {
        scope.launch {
            try {
                val stackTrace = getStackTraceAsString(throwable)
                val errorMessage = throwable.message ?: "No error message"
                
                val logEntity = com.yourapp.sqliautohunter.data.local.database.entity.CrashLogEntity(
                    timestamp = System.currentTimeMillis(),
                    moduleName = "Global",
                    errorMessage = "$errorMessage (Thread: ${thread.name})",
                    stackTrace = stackTrace,
                    urlBeingProcessed = null,
                    severity = "Fatal"
                )
                
                crashLogRepository.saveLog(logEntity)
                
            } catch (e: Exception) {
                Log.e(tag, "Failed to log exception to database", e)
            }
        }
    }

    private fun getStackTraceAsString(throwable: Throwable): String {
        val stringWriter = StringWriter()
        val printWriter = PrintWriter(stringWriter)
        throwable.printStackTrace(printWriter)
        printWriter.close()
        return stringWriter.toString()
    }

    // Method to wrap coroutines with exception handling
    fun <T> wrapWithExceptionHandling(block: suspend () -> T): suspend () -> T {
        return {
            try {
                block()
            } catch (e: Exception) {
                uncaughtException(Thread.currentThread(), e)
                throw e
            }
        }
    }

    // Method to wrap coroutines and log exceptions without rethrowing
    fun <T> wrapAndLogExceptions(block: suspend () -> T): suspend () -> T? {
        return {
            try {
                block()
            } catch (e: Exception) {
                uncaughtException(Thread.currentThread(), e)
                null
            }
        }
    }

    // Method to log an exception from anywhere in the app
    suspend fun logException(
        throwable: Throwable,
        module: String = "Unknown",
        context: String = "",
        url: String? = null
    ) {
        try {
            val stackTrace = getStackTraceAsString(throwable)
            val errorMessage = throwable.message ?: "No error message"
            
            val logEntity = com.yourapp.sqliautohunter.data.local.database.entity.CrashLogEntity(
                timestamp = System.currentTimeMillis(),
                moduleName = module,
                errorMessage = "$errorMessage - $context",
                stackTrace = stackTrace,
                urlBeingProcessed = url,
                severity = "Error"
            )
            
            crashLogRepository.saveLog(logEntity)
            
        } catch (e: Exception) {
            Log.e(tag, "Failed to log exception", e)
        }
    }

    fun shutdown() {
        scope.cancel()
    }
}
