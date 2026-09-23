package com.yourapp.sqliautohunter.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourapp.sqliautohunter.data.repository.QueueRepository
import com.yourapp.sqliautohunter.data.repository.ScanResultRepository
import com.yourapp.sqliautohunter.engine.concurrency.ScanWorkerPool
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LiveDashboardViewModel @Inject constructor(
    private val queueRepository: QueueRepository,
    private val scanResultRepository: ScanResultRepository,
    private val scanWorkerPool: ScanWorkerPool
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    private val _autoRefresh = MutableStateFlow(true)
    val autoRefresh: StateFlow<Boolean> = _autoRefresh.asStateFlow()

    init {
        startAutoRefresh()
    }

    private fun startAutoRefresh() {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                delay(2000) // Refresh every 2 seconds
                if (_autoRefresh.value) {
                    refreshStats()
                }
            }
        }
    }

    private suspend fun refreshStats() {
        try {
            val queueStats = queueRepository.getQueueStats().first()
            val resultStats = scanResultRepository.getResultStats().first()
            val workerStats = scanWorkerPool.getStats()

            _state.value = _state.value.copy(
                queueStats = queueStats,
                resultStats = resultStats,
                workerStats = workerStats,
                lastUpdated = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun toggleAutoRefresh() {
        _autoRefresh.value = !_autoRefresh.value
    }

    fun refreshNow() {
        viewModelScope.launch(Dispatchers.IO) {
            refreshStats()
        }
    }

    fun startScan() {
        viewModelScope.launch(Dispatchers.IO) {
            scanWorkerPool.start()
        }
    }

    fun pauseScan() {
        viewModelScope.launch(Dispatchers.IO) {
            scanWorkerPool.pause()
        }
    }

    fun resumeScan() {
        viewModelScope.launch(Dispatchers.IO) {
            scanWorkerPool.resume()
        }
    }

    fun stopScan() {
        viewModelScope.launch(Dispatchers.IO) {
            scanWorkerPool.stop()
        }
    }

    fun clearQueue() {
        viewModelScope.launch(Dispatchers.IO) {
            queueRepository.clearQueue()
        }
    }

    fun clearResults() {
        viewModelScope.launch(Dispatchers.IO) {
            scanResultRepository.clearResults()
        }
    }

    data class DashboardState(
        val queueStats: QueueRepository.QueueStats = QueueRepository.QueueStats(0, 0, 0, 0, 0, 0),
        val resultStats: ScanResultRepository.ResultStats = ScanResultRepository.ResultStats(
            total = 0,
            byType = emptyMap(),
            byConfidence = emptyMap()
        ),
        val workerStats: ScanWorkerPool.WorkerPoolStats = ScanWorkerPool.WorkerPoolStats(
            isRunning = false,
            isPaused = false,
            processedCount = 0,
            vulnerableCount = 0,
            errorCount = 0,
            lastError = null,
            activeWorkers = 0,
            maxWorkers = 0
        ),
        val lastUpdated: Long = System.currentTimeMillis(),
        val isRefreshing: Boolean = false
    )
}
