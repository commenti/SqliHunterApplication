package com.yourapp.sqliautohunter.ui.screens.keywordinput

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourapp.sqliautohunter.data.repository.QueueRepository
import com.yourapp.sqliautohunter.data.repository.ScanResultRepository
import com.yourapp.sqliautohunter.domain.model.DorkTemplate
import com.yourapp.sqliautohunter.domain.usecase.GenerateDorkQueriesUseCase
import com.yourapp.sqliautohunter.domain.usecase.ScrapeSearchResultsUseCase
import com.yourapp.sqliautohunter.engine.concurrency.ScanWorkerPool
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class KeywordInputViewModel @Inject constructor(
    private val generateDorkQueriesUseCase: GenerateDorkQueriesUseCase,
    private val scrapeSearchResultsUseCase: ScrapeSearchResultsUseCase,
    private val queueRepository: QueueRepository,
    private val scanWorkerPool: ScanWorkerPool
) : ViewModel() {

    private val _state = MutableStateFlow(KeywordInputState())
    val state: StateFlow<KeywordInputState> = _state.asStateFlow()

    private val _keywords = MutableStateFlow("")
    val keywords: StateFlow<String> = _keywords.asStateFlow()

    private val _selectedTemplateIndex = MutableStateFlow(0)
    val selectedTemplateIndex: StateFlow<Int> = _selectedTemplateIndex.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanProgress = MutableStateFlow(ScanProgress(0, 0, 0))
    val scanProgress: StateFlow<ScanProgress> = _scanProgress.asStateFlow()

    val templates = DorkTemplate.DEFAULT_TEMPLATES

    init {
        viewModelScope.launch {
            combine(
                _keywords,
                _selectedTemplateIndex,
                _isScanning,
                _scanProgress
            ) { keyword, templateIndex, scanning, progress ->
                KeywordInputState(
                    keywords = keyword,
                    selectedTemplateIndex = templateIndex,
                    selectedTemplate = templates.getOrNull(templateIndex),
                    isScanning = scanning,
                    scanProgress = progress
                )
            }.collect { state ->
                _state.value = state
            }
        }
    }

    fun onKeywordsChanged(keywords: String) {
        _keywords.value = keywords
    }

    fun onTemplateSelected(index: Int) {
        _selectedTemplateIndex.value = index
    }

    fun startScan() {
        viewModelScope.launch(Dispatchers.IO) {
            val keywordText = _keywords.value.trim()
            
            if (keywordText.isEmpty()) {
                _state.value = _state.value.copy(error = "Please enter keywords")
                return@launch
            }

            _isScanning.value = true
            _scanProgress.value = ScanProgress(0, 0, 0)
            _state.value = _state.value.copy(error = null)

            try {
                // Generate dork queries
                val keywords = keywordText.split(",", "\n", ";").map { it.trim() }.filter { it.isNotEmpty() }
                val selectedTemplate = templates.getOrNull(_selectedTemplateIndex.value)
                
                val queries = if (selectedTemplate != null) {
                    generateDorkQueriesUseCase(keywords, listOf(selectedTemplate))
                } else {
                    generateDorkQueriesUseCase(keywords)
                }

                // Scrape search results
                val urls = mutableListOf<String>()
                for (query in queries.take(5)) { // Limit to 5 queries for demo
                    val results = scrapeSearchResultsUseCase(query, maxResults = 10)
                    urls.addAll(results)
                    
                    _scanProgress.value = _scanProgress.value.copy(
                        queriesProcessed = _scanProgress.value.queriesProcessed + 1
                    )
                }

                // Add URLs to queue
                if (urls.isNotEmpty()) {
                    val addedCount = queueRepository.addToQueue(urls, keywordText)
                    _scanProgress.value = _scanProgress.value.copy(
                        urlsFound = urls.size,
                        urlsAdded = addedCount
                    )

                    // Start worker pool
                    scanWorkerPool.start()
                }

                _isScanning.value = false
                
            } catch (e: Exception) {
                _isScanning.value = false
                _state.value = _state.value.copy(error = e.message ?: "Scan failed")
            }
        }
    }

    fun stopScan() {
        viewModelScope.launch {
            scanWorkerPool.stop()
            _isScanning.value = false
        }
    }

    fun pauseScan() {
        viewModelScope.launch {
            scanWorkerPool.pause()
        }
    }

    fun resumeScan() {
        viewModelScope.launch {
            scanWorkerPool.resume()
        }
    }

    fun clearKeywords() {
        _keywords.value = ""
    }

    data class KeywordInputState(
        val keywords: String = "",
        val selectedTemplateIndex: Int = 0,
        val selectedTemplate: DorkTemplate? = null,
        val isScanning: Boolean = false,
        val scanProgress: ScanProgress = ScanProgress(0, 0, 0),
        val error: String? = null
    )

    data class ScanProgress(
        val queriesProcessed: Int,
        val urlsFound: Int,
        val urlsAdded: Int
    )
}
