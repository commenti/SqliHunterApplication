package com.yourapp.sqliautohunter.data.repository

import android.content.Context
import com.yourapp.sqliautohunter.data.local.preferences.SettingsDataStore
import com.yourapp.sqliautohunter.util.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

class SettingsRepository(private val context: Context) {

    private val settingsDataStore = SettingsDataStore(context)

    // Thread count
    val threadCountFlow: Flow<Int> = settingsDataStore.threadCountFlow
    suspend fun getThreadCount(): Int = settingsDataStore.threadCountFlow.first()
    suspend fun setThreadCount(value: Int) = settingsDataStore.setThreadCount(value)

    // Delay
    val delayMsFlow: Flow<Long> = settingsDataStore.delayMsFlow
    suspend fun getDelayMs(): Long = settingsDataStore.delayMsFlow.first()
    suspend fun setDelayMs(value: Long) = settingsDataStore.setDelayMs(value)

    // Bulk mode
    val bulkModeFlow: Flow<Boolean> = settingsDataStore.bulkModeFlow
    suspend fun getBulkMode(): Boolean = settingsDataStore.bulkModeFlow.first()
    suspend fun setBulkMode(value: Boolean) = settingsDataStore.setBulkMode(value)

    // Proxy list
    val proxyListFlow: Flow<String> = settingsDataStore.proxyListFlow
    suspend fun getProxyList(): String = settingsDataStore.proxyListFlow.first()
    suspend fun setProxyList(value: String) = settingsDataStore.setProxyList(value)

    // Auto export
    val autoExportFlow: Flow<Boolean> = settingsDataStore.autoExportFlow
    suspend fun getAutoExport(): Boolean = settingsDataStore.autoExportFlow.first()
    suspend fun setAutoExport(value: Boolean) = settingsDataStore.setAutoExport(value)

    // Blacklist
    val blacklistFlow: Flow<String> = settingsDataStore.blacklistFlow
    suspend fun getBlacklist(): String = settingsDataStore.blacklistFlow.first()
    suspend fun setBlacklist(value: String) = settingsDataStore.setBlacklist(value)

    fun getAllSettingsFlow(): Flow<Map<String, Any>> {
        @Suppress("UNCHECKED_CAST")
        return combine(
            settingsDataStore.threadCountFlow,
            settingsDataStore.delayMsFlow,
            settingsDataStore.bulkModeFlow,
            settingsDataStore.proxyListFlow,
            settingsDataStore.autoExportFlow,
            settingsDataStore.blacklistFlow
        ) { args: Array<Any?> ->
            mapOf(
                Constants.SETTING_THREAD_COUNT to (args[0] as Int),
                Constants.SETTING_DELAY_MS to (args[1] as Long),
                Constants.SETTING_BULK_MODE to (args[2] as Boolean),
                Constants.SETTING_PROXY_LIST to (args[3] as String),
                Constants.SETTING_AUTO_EXPORT to (args[4] as Boolean),
                "blacklist_domains" to (args[5] as String)
            )
        }
    }

    suspend fun getAllSettings(): Map<String, Any> = settingsDataStore.getAllSettings()

    suspend fun resetToDefaults() {
        settingsDataStore.setThreadCount(Constants.SETTING_DEFAULT_THREAD_COUNT)
        settingsDataStore.setDelayMs(Constants.SETTING_DEFAULT_DELAY_MS)
        settingsDataStore.setBulkMode(Constants.SETTING_DEFAULT_BULK_MODE)
        settingsDataStore.setProxyList("")
        settingsDataStore.setAutoExport(false)
        settingsDataStore.setBlacklist(Constants.DEFAULT_BLACKLIST_DOMAINS.joinToString(","))
    }

    suspend fun clearAll() = settingsDataStore.clearAll()

    // Helper methods for specific settings
    suspend fun getMaxConcurrency(): Int {
        val threadCount = getThreadCount()
        return when {
            threadCount <= 2 -> Constants.MAX_CONCURRENCY_LOW_RAM
            threadCount <= 4 -> Constants.MAX_CONCURRENCY_MEDIUM_RAM
            else -> Constants.MAX_CONCURRENCY_HIGH_RAM
        }
    }

    suspend fun getProxyListAsList(): List<String> {
        val proxyList = getProxyList()
        return if (proxyList.isEmpty()) emptyList() else proxyList.split(",")
    }

    suspend fun getBlacklistAsList(): List<String> {
        val blacklist = getBlacklist()
        return if (blacklist.isEmpty()) Constants.DEFAULT_BLACKLIST_DOMAINS else blacklist.split(",")
    }
}
