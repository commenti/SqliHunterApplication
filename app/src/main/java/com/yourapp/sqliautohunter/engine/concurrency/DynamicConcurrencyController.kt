package com.yourapp.sqliautohunter.engine.concurrency

import android.content.Context
import com.yourapp.sqliautohunter.data.repository.SettingsRepository
import com.yourapp.sqliautohunter.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class DynamicConcurrencyController(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val deviceRamDetector: DeviceRamDetector = DeviceRamDetector(context)
) {

    private val mutex = Mutex()
    private var currentMaxConcurrency: Int = Constants.DEFAULT_CONCURRENCY
    private var activeTasks: Int = 0
    private var lastAdjustmentTime: Long = 0

    init {
        // Initialize with device RAM detection
        updateMaxConcurrency()
    }

    suspend fun acquireSlot(): Boolean {
        return mutex.withLock {
            if (activeTasks < currentMaxConcurrency) {
                activeTasks++
                true
            } else {
                false
            }
        }
    }

    suspend fun releaseSlot() {
        mutex.withLock {
            activeTasks = maxOf(activeTasks - 1, 0)
        }
    }

    suspend fun getCurrentMaxConcurrency(): Int {
        return mutex.withLock {
            // Check if we need to update based on settings or RAM
            if (shouldUpdateConcurrency()) {
                updateMaxConcurrency()
            }
            currentMaxConcurrency
        }
    }

    suspend fun getActiveTasks(): Int {
        return mutex.withLock {
            activeTasks
        }
    }

    suspend fun getAvailableSlots(): Int {
        return mutex.withLock {
            maxOf(currentMaxConcurrency - activeTasks, 0)
        }
    }

    private suspend fun shouldUpdateConcurrency(): Boolean {
        val now = System.currentTimeMillis()
        val settingsThreadCount = settingsRepository.getThreadCount()
        val ramRecommended = deviceRamDetector.getRecommendedConcurrency()
        
        // Update if settings changed or RAM category changed
        val settingsChanged = settingsThreadCount != currentMaxConcurrency
        val ramChanged = ramRecommended != currentMaxConcurrency
        val timeElapsed = now - lastAdjustmentTime > 60000 // 1 minute
        
        return settingsChanged || ramChanged || timeElapsed
    }

    private suspend fun updateMaxConcurrency() {
        mutex.withLock {
            val settingsThreadCount = settingsRepository.getThreadCount()
            val ramRecommended = deviceRamDetector.getRecommendedConcurrency()
            
            // Use the minimum of settings and RAM recommendation
            currentMaxConcurrency = minOf(settingsThreadCount, ramRecommended)
            
            // Ensure at least 1
            currentMaxConcurrency = maxOf(currentMaxConcurrency, 1)
            
            lastAdjustmentTime = System.currentTimeMillis()
        }
    }

    suspend fun setMaxConcurrency(value: Int) {
        mutex.withLock {
            currentMaxConcurrency = maxOf(value, 1)
            settingsRepository.setThreadCount(currentMaxConcurrency)
            lastAdjustmentTime = System.currentTimeMillis()
        }
    }

    suspend fun adjustBasedOnPerformance(successRate: Double) {
        mutex.withLock {
            // If success rate is low, reduce concurrency
            if (successRate < 0.7 && currentMaxConcurrency > 1) {
                currentMaxConcurrency = maxOf(currentMaxConcurrency - 1, 1)
                lastAdjustmentTime = System.currentTimeMillis()
            }
            // If success rate is high, try to increase
            else if (successRate > 0.95 && currentMaxConcurrency < deviceRamDetector.getRecommendedConcurrency()) {
                currentMaxConcurrency = minOf(currentMaxConcurrency + 1, deviceRamDetector.getRecommendedConcurrency())
                lastAdjustmentTime = System.currentTimeMillis()
            }
        }
    }

    suspend fun waitForAvailableSlot(timeoutMs: Long = 30000): Boolean {
        val startTime = System.currentTimeMillis()
        
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (acquireSlot()) {
                return true
            }
            
            // Wait a bit before retrying
            kotlinx.coroutines.delay(100)
        }
        
        return false
    }

    fun getConcurrencyInfo(): ConcurrencyInfo {
        return ConcurrencyInfo(
            maxConcurrency = currentMaxConcurrency,
            activeTasks = activeTasks,
            availableSlots = maxOf(currentMaxConcurrency - activeTasks, 0),
            ramCategory = deviceRamDetector.getRamCategory(),
            ramRecommended = deviceRamDetector.getRecommendedConcurrency()
        )
    }

    data class ConcurrencyInfo(
        val maxConcurrency: Int,
        val activeTasks: Int,
        val availableSlots: Int,
        val ramCategory: DeviceRamDetector.RamCategory,
        val ramRecommended: Int
    )
}
