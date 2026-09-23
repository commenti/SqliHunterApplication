package com.yourapp.sqliautohunter.ui.screens.results

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourapp.sqliautohunter.data.local.database.entity.VulnerabilityResultEntity
import com.yourapp.sqliautohunter.data.repository.ScanResultRepository
import com.yourapp.sqliautohunter.domain.model.ConfidenceLevel
import com.yourapp.sqliautohunter.domain.model.VulnerabilityType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ResultsViewModel @Inject constructor(
    private val scanResultRepository: ScanResultRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ResultsState())
    val state: StateFlow<ResultsState> = _state.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedConfidence = MutableStateFlow<ConfidenceLevel?>(null)
    val selectedConfidence: StateFlow<ConfidenceLevel?> = _selectedConfidence.asStateFlow()

    private val _selectedType = MutableStateFlow<VulnerabilityType?>(null)
    val selectedType: StateFlow<VulnerabilityType?> = _selectedType.asStateFlow()

    private val _sortBy = MutableStateFlow(SortBy.DATE_DESCENDING)
    val sortBy: StateFlow<SortBy> = _sortBy.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            combine(
                scanResultRepository.getAllResults(),
                _searchQuery,
                _selectedConfidence,
                _selectedType,
                _sortBy
            ) { results, query, confidence, type, sort ->
                val filtered = filterResults(results, query, confidence, type)
                val sorted = sortResults(filtered, sort)
                ResultsState(results = sorted, totalCount = results.size, filteredCount = filtered.size)
            }.collect { newState -> _state.value = newState }
        }
    }

    private fun filterResults(
        results: List<VulnerabilityResultEntity>,
        query: String,
        confidence: ConfidenceLevel?,
        type: VulnerabilityType?
    ): List<VulnerabilityResultEntity> {
        return results.filter { result ->
            val matchesQuery = query.isEmpty() || 
                    result.url.contains(query, ignoreCase = true) ||
                    result.keywordSource.contains(query, ignoreCase = true)
            
            val matchesConfidence = confidence == null || result.confidence == confidence
            val matchesType = type == null || result.vulnType == type
            
            matchesQuery && matchesConfidence && matchesType
        }
    }

    private fun sortResults(
        results: List<VulnerabilityResultEntity>,
        sortBy: SortBy
    ): List<VulnerabilityResultEntity> {
        return when (sortBy) {
            SortBy.DATE_DESCENDING -> results.sortedByDescending { it.discoveredAt }
            SortBy.DATE_ASCENDING -> results.sortedBy { it.discoveredAt }
            SortBy.CONFIDENCE_HIGH -> results.sortedByDescending { 
                when (it.confidence) {
                    ConfidenceLevel.HIGH -> 3
                    ConfidenceLevel.MEDIUM -> 2
                    ConfidenceLevel.LOW -> 1
                }
            }
            SortBy.CONFIDENCE_LOW -> results.sortedBy { 
                when (it.confidence) {
                    ConfidenceLevel.HIGH -> 3
                    ConfidenceLevel.MEDIUM -> 2
                    ConfidenceLevel.LOW -> 1
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onConfidenceSelected(confidence: ConfidenceLevel?) {
        _selectedConfidence.value = confidence
    }

    fun onTypeSelected(type: VulnerabilityType?) {
        _selectedType.value = type
    }

    fun onSortByChanged(sortBy: SortBy) {
        _sortBy.value = sortBy
    }

    fun clearFilters() {
        _searchQuery.value = ""
        _selectedConfidence.value = null
        _selectedType.value = null
        _sortBy.value = SortBy.DATE_DESCENDING
    }

    fun deleteResult(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            scanResultRepository.deleteResult(id)
        }
    }

    fun clearAllResults() {
        viewModelScope.launch(Dispatchers.IO) {
            scanResultRepository.clearResults()
        }
    }

    fun exportResults() {
        // This would be handled by ExportResultsToCsvUseCase
    }

    enum class SortBy {
        DATE_DESCENDING,
        DATE_ASCENDING,
        CONFIDENCE_HIGH,
        CONFIDENCE_LOW
    }

    data class ResultsState(
        val results: List<VulnerabilityResultEntity> = emptyList(),
        val totalCount: Int = 0,
        val filteredCount: Int = 0,
        val isLoading: Boolean = false,
        val error: String? = null
    )
}
