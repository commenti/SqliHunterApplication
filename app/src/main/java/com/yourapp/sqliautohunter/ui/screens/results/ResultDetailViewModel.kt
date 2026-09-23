package com.yourapp.sqliautohunter.ui.screens.results

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourapp.sqliautohunter.data.repository.ScanResultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ResultDetailViewModel @Inject constructor(
    private val scanResultRepository: ScanResultRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(ResultDetailState())
    val state: StateFlow<ResultDetailState> = _state.asStateFlow()

    private val resultId: Long = savedStateHandle.get<Long>("result_id") ?: 0L

    init {
        loadResult()
    }

    private fun loadResult() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = scanResultRepository.getRecentResults(1).firstOrNull()
                
                if (result != null) {
                    _state.value = _state.value.copy(
                        result = result,
                        isLoading = false
                    )
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Result not found"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load result"
                )
            }
        }
    }

    fun deleteResult() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                scanResultRepository.deleteResult(resultId)
                _state.value = _state.value.copy(
                    result = null,
                    deleted = true
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = e.message ?: "Failed to delete result"
                )
            }
        }
    }

    data class ResultDetailState(
        val result: com.yourapp.sqliautohunter.data.local.database.entity.VulnerabilityResultEntity? = null,
        val isLoading: Boolean = true,
        val error: String? = null,
        val deleted: Boolean = false
    )
}
