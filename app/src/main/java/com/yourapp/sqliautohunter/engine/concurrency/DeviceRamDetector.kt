package com.yourapp.sqliautohunter.engine.concurrency

import android.app.ActivityManager
import android.content.Context
import com.yourapp.sqliautohunter.util.Constants

class DeviceRamDetector(private val context: Context) {

    fun getTotalRam(): Long {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)
            memoryInfo.totalMem
        } catch (e: Exception) {
            // Fallback: assume medium RAM if we can't detect
            4L * 1024 * 1024 * 1024 // 4GB in bytes
        }
    }

    fun getAvailableRam(): Long {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)
            memoryInfo.availMem
        } catch (e: Exception) {
            2L * 1024 * 1024 * 1024 // 2GB in bytes
        }
    }

    fun getRamInGB(): Double {
        return getTotalRam().toDouble() / (1024 * 1024 * 1024)
    }

    fun getRamCategory(): RamCategory {
        val ramGB = getRamInGB()
        
        return when {
            ramGB < 3 -> RamCategory.LOW
            ramGB < 6 -> RamCategory.MEDIUM
            else -> RamCategory.HIGH
        }
    }

    fun getRecommendedConcurrency(): Int {
        return when (getRamCategory()) {
            RamCategory.LOW -> Constants.MAX_CONCURRENCY_LOW_RAM
            RamCategory.MEDIUM -> Constants.MAX_CONCURRENCY_MEDIUM_RAM
            RamCategory.HIGH -> Constants.MAX_CONCURRENCY_HIGH_RAM
        }
    }

    fun isLowRamDevice(): Boolean {
        return getRamCategory() == RamCategory.LOW
    }

    fun getRamInfo(): RamInfo {
        return RamInfo(
            totalRamBytes = getTotalRam(),
            availableRamBytes = getAvailableRam(),
            totalRamGB = getRamInGB(),
            availableRamGB = getAvailableRam().toDouble() / (1024 * 1024 * 1024),
            category = getRamCategory(),
            recommendedConcurrency = getRecommendedConcurrency()
        )
    }

    enum class RamCategory {
        LOW,
        MEDIUM,
        HIGH
    }

    data class RamInfo(
        val totalRamBytes: Long,
        val availableRamBytes: Long,
        val totalRamGB: Double,
        val availableRamGB: Double,
        val category: RamCategory,
        val recommendedConcurrency: Int
    )
}
