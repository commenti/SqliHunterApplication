package com.yourapp.sqliautohunter.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourapp.sqliautohunter.data.repository.SettingsRepository
import com.yourapp.sqliautohunter.engine.concurrency.DeviceRamDetector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val deviceRamDetector: DeviceRamDetector
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val settings = settingsRepository.getAllSettings()
                val ramInfo = deviceRamDetector.getRamInfo()

                _state.value = _state.value.copy(
                    threadCount = settings["thread_count"] as? Int ?: 4,
                    delayMs = settings["delay_ms"] as? Long ?: 1000L,
                    bulkMode = settings["bulk_mode"] as? Boolean ?: false,
                    proxyList = settings["proxy_list"] as? String ?: "",
                    autoExport = settings["auto_export"] as? Boolean ?: false,
                    blacklist = settings["blacklist_domains"] as? String ?: "",
                    ramInfo = ramInfo,
                    isLoading = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load settings"
                )
            }
        }
    }

    fun onThreadCountChanged(value: Int) {
        _state.value = _state.value.copy(threadCount = value)
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.setThreadCount(value)
        }
    }

    fun onDelayMsChanged(value: Long) {
        _state.value = _state.value.copy(delayMs = value)
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.setDelayMs(value)
        }
    }

    fun onBulkModeChanged(value: Boolean) {
        _state.value = _state.value.copy(bulkMode = value)
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.setBulkMode(value)
        }
    }

    fun onProxyListChanged(value: String) {
        _state.value = _state.value.copy(proxyList = value)
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.setProxyList(value)
        }
    }

    fun onAutoExportChanged(value: Boolean) {
        _state.value = _state.value.copy(autoExport = value)
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.setAutoExport(value)
        }
    }

    fun onBlacklistChanged(value: String) {
        _state.value = _state.value.copy(blacklist = value)
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.setBlacklist(value)
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.resetToDefaults()
            loadSettings()
        }
    }

    fun saveAll() {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.setThreadCount(_state.value.threadCount)
            settingsRepository.setDelayMs(_state.value.delayMs)
            settingsRepository.setBulkMode(_state.value.bulkMode)
            settingsRepository.setProxyList(_state.value.proxyList)
            settingsRepository.setAutoExport(_state.value.autoExport)
            settingsRepository.setBlacklist(_state.value.blacklist)
        }
    }

    data class SettingsState(
        val threadCount: Int = 4,
        val delayMs: Long = 1000L,
        val bulkMode: Boolean = false,
        val proxyList: String = "",
        val autoExport: Boolean = false,
        val blacklist: String = "",
        val ramInfo: DeviceRamDetector.RamInfo? = null,
        val isLoading: Boolean = true,
        val error: String? = null
    )
}
