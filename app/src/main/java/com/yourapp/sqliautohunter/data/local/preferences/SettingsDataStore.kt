package com.yourapp.sqliautohunter.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.yourapp.sqliautohunter.util.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = Constants.SETTINGS_NAME)

class SettingsDataStore(private val context: Context) {

    private val dataStore = context.dataStore

    // Thread count
    private val threadCountKey = intPreferencesKey(Constants.SETTING_THREAD_COUNT)
    val threadCountFlow: Flow<Int> = dataStore.data
        .map { preferences ->
            preferences[threadCountKey] ?: Constants.SETTING_DEFAULT_THREAD_COUNT
        }

    suspend fun setThreadCount(value: Int) {
        dataStore.edit { preferences ->
            preferences[threadCountKey] = value
        }
    }

    // Delay between requests
    private val delayMsKey = longPreferencesKey(Constants.SETTING_DELAY_MS)
    val delayMsFlow: Flow<Long> = dataStore.data
        .map { preferences ->
            preferences[delayMsKey] ?: Constants.SETTING_DEFAULT_DELAY_MS
        }

    suspend fun setDelayMs(value: Long) {
        dataStore.edit { preferences ->
            preferences[delayMsKey] = value
        }
    }

    // Bulk mode
    private val bulkModeKey = booleanPreferencesKey(Constants.SETTING_BULK_MODE)
    val bulkModeFlow: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[bulkModeKey] ?: Constants.SETTING_DEFAULT_BULK_MODE
        }

    suspend fun setBulkMode(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[bulkModeKey] = value
        }
    }

    // Proxy list
    private val proxyListKey = stringPreferencesKey(Constants.SETTING_PROXY_LIST)
    val proxyListFlow: Flow<String> = dataStore.data
        .map { preferences ->
            preferences[proxyListKey] ?: ""
        }

    suspend fun setProxyList(value: String) {
        dataStore.edit { preferences ->
            preferences[proxyListKey] = value
        }
    }

    // Auto export
    private val autoExportKey = booleanPreferencesKey(Constants.SETTING_AUTO_EXPORT)
    val autoExportFlow: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[autoExportKey] ?: false
        }

    suspend fun setAutoExport(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[autoExportKey] = value
        }
    }

    // Blacklist domains
    private val blacklistKey = stringPreferencesKey("blacklist_domains")
    val blacklistFlow: Flow<String> = dataStore.data
        .map { preferences ->
            preferences[blacklistKey] ?: Constants.DEFAULT_BLACKLIST_DOMAINS.joinToString(",")
        }

    suspend fun setBlacklist(value: String) {
        dataStore.edit { preferences ->
            preferences[blacklistKey] = value
        }
    }

    // Clear all settings
    suspend fun clearAll() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    // Get all settings as a map
    suspend fun getAllSettings(): Map<String, Any> {
        val prefs = dataStore.data.first()
        return mapOf(
            Constants.SETTING_THREAD_COUNT to (prefs[threadCountKey] ?: Constants.SETTING_DEFAULT_THREAD_COUNT),
            Constants.SETTING_DELAY_MS to (prefs[delayMsKey] ?: Constants.SETTING_DEFAULT_DELAY_MS),
            Constants.SETTING_BULK_MODE to (prefs[bulkModeKey] ?: Constants.SETTING_DEFAULT_BULK_MODE),
            Constants.SETTING_PROXY_LIST to (prefs[proxyListKey] ?: ""),
            Constants.SETTING_AUTO_EXPORT to (prefs[autoExportKey] ?: false),
            "blacklist_domains" to (prefs[blacklistKey] ?: Constants.DEFAULT_BLACKLIST_DOMAINS.joinToString(","))
        )
    }
}
