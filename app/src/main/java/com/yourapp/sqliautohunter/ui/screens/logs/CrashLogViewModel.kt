package com.yourapp.sqliautohunter.ui.screens.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourapp.sqliautohunter.data.repository.CrashLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CrashLogViewModel @Inject constructor(
    private val crashLogRepository: CrashLogRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CrashLogState())
    val state: StateFlow<CrashLogState> = _state.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedModule = MutableStateFlow<String?>(null)
    val selectedModule: StateFlow<String?> = _selectedModule.asStateFlow()

    private val _selectedSeverity = MutableStateFlow<String?>(null)
    val selectedSeverity: StateFlow<String?> = _selectedSeverity.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            combine(
                crashLogRepository.getAllLogs(),
                _searchQuery,
                _selectedModule,
                _selectedSeverity
            ) { logs, query, module, severity ->
                val filtered = filterLogs(logs, query, module, severity)
                CrashLogState(logs = filtered, totalCount = logs.size, filteredCount = filtered.size)
            }.collect { newState -> _state.value = newState }
        }
    }

    private fun filterLogs(
        logs: List<com.yourapp.sqliautohunter.data.local.database.entity.CrashLogEntity>,
        query: String,
        module: String?,
        severity: String?
    ): List<com.yourapp.sqliautohunter.data.local.database.entity.CrashLogEntity> {
        return logs.filter { log ->
            val matchesQuery = query.isEmpty() || 
                    log.errorMessage.contains(query, ignoreCase = true) ||
                    log.stackTrace.contains(query, ignoreCase = true) ||
                    log.moduleName.contains(query, ignoreCase = true)
            
            val matchesModule = module == null || log.moduleName == module
            val matchesSeverity = severity == null || log.severity == severity
            
            matchesQuery && matchesModule && matchesSeverity
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onModuleSelected(module: String?) {
        _selectedModule.value = module
    }

    fun onSeveritySelected(severity: String?) {
        _selectedSeverity.value = severity
    }

    fun clearFilters() {
        _searchQuery.value = ""
        _selectedModule.value = null
        _selectedSeverity.value = null
    }

    fun deleteLog(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            crashLogRepository.deleteLog(id)
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            crashLogRepository.clearLogs()
        }
    }

    fun exportLogs() {
        // This would be handled by a file export function
    }

    data class CrashLogState(
        val logs: List<com.yourapp.sqliautohunter.data.local.database.entity.CrashLogEntity> = emptyList(),
        val totalCount: Int = 0,
        val filteredCount: Int = 0,
        val isLoading: Boolean = false,
        val error: String? = null
    )
}
