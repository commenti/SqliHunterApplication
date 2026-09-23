package com.yourapp.sqliautohunter.ui.screens.manualtest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourapp.sqliautohunter.domain.usecase.CheckUrlHashUseCase
import com.yourapp.sqliautohunter.domain.usecase.FilterBlacklistedDomainUseCase
import com.yourapp.sqliautohunter.domain.usecase.NormalizeUrlUseCase
import com.yourapp.sqliautohunter.domain.usecase.ScanUrlForVulnerabilityUseCase
import com.yourapp.sqliautohunter.engine.browser.PayloadInjector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManualUrlTestViewModel @Inject constructor(
    private val scanUrlForVulnerabilityUseCase: ScanUrlForVulnerabilityUseCase,
    private val normalizeUrlUseCase: NormalizeUrlUseCase,
    private val filterBlacklistedDomainUseCase: FilterBlacklistedDomainUseCase,
    private val checkUrlHashUseCase: CheckUrlHashUseCase,
    private val payloadInjector: PayloadInjector
) : ViewModel() {

    private val _state = MutableStateFlow(ManualTestState())
    val state: StateFlow<ManualTestState> = _state.asStateFlow()

    private val _url = MutableStateFlow("")
    val url: StateFlow<String> = _url.asStateFlow()

    private val _isTesting = MutableStateFlow(false)
    val isTesting: StateFlow<Boolean> = _isTesting.asStateFlow()

    init {
        viewModelScope.launch {
            _url.collect { urlValue ->
                _state.value = _state.value.copy(url = urlValue)
            }
        }
    }

    fun onUrlChanged(url: String) {
        _url.value = url
    }

    fun testUrl() {
        viewModelScope.launch(Dispatchers.IO) {
            val urlText = _url.value.trim()
            
            if (urlText.isEmpty()) {
                _state.value = _state.value.copy(error = "Please enter a URL")
                return@launch
            }

            // Validate URL
            if (!normalizeUrlUseCase.isValidUrl(urlText)) {
                _state.value = _state.value.copy(error = "Invalid URL format")
                return@launch
            }

            // Check if blacklisted
            if (!filterBlacklistedDomainUseCase(urlText)) {
                _state.value = _state.value.copy(error = "Domain is blacklisted")
                return@launch
            }

            // Check if already tested
            if (checkUrlHashUseCase(urlText)) {
                val result = checkUrlHashUseCase.getTestResult(urlText)
                _state.value = _state.value.copy(
                    isAlreadyTested = true,
                    previousResult = result,
                    error = null
                )
                return@launch
            }

            _isTesting.value = true
            _state.value = _state.value.copy(
                error = null,
                isAlreadyTested = false,
                previousResult = null
            )

            try {
                // Perform the scan
                val scanResult = scanUrlForVulnerabilityUseCase(urlText, "manual")
                
                _state.value = _state.value.copy(
                    isVulnerable = scanResult.isVulnerable,
                    confidence = scanResult.confidence,
                    vulnerabilityTypes = scanResult.vulnerabilityTypes,
                    detectionResults = scanResult.detectionResults,
                    isAlreadyTested = false,
                    previousResult = null
                )

                // Mark as tested
                checkUrlHashUseCase.checkAndInsert(
                    urlText,
                    if (scanResult.isVulnerable) "vulnerable" else "not_vulnerable",
                    scanResult.vulnerabilityTypes.joinToString(",")
                )

            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message ?: "Test failed")
            } finally {
                _isTesting.value = false
            }
        }
    }

    fun quickTest() {
        viewModelScope.launch(Dispatchers.IO) {
            val urlText = _url.value.trim()
            
            if (urlText.isEmpty()) {
                _state.value = _state.value.copy(error = "Please enter a URL")
                return@launch
            }

            _isTesting.value = true
            _state.value = _state.value.copy(error = null)

            try {
                // Use payload injector for quick test
                val result = payloadInjector.quickTest(urlText)
                
                _state.value = _state.value.copy(
                    isVulnerable = result.isVulnerable,
                    confidence = com.yourapp.sqliautohunter.domain.model.ConfidenceLevel.valueOf(
                        when {
                            result.confidence >= 0.8 -> "HIGH"
                            result.confidence >= 0.5 -> "MEDIUM"
                            else -> "LOW"
                        }
                    ),
                    vulnerabilityTypes = listOf(
                        com.yourapp.sqliautohunter.domain.model.VulnerabilityType.valueOf(
                            result.vulnerabilityType.uppercase()
                        )
                    ),
                    detectionResults = listOf(
                        ScanUrlForVulnerabilityUseCase.DetectionResult(
                            type = com.yourapp.sqliautohunter.domain.model.VulnerabilityType.valueOf(
                                result.vulnerabilityType.uppercase()
                            ),
                            payload = result.payload,
                            isVulnerable = result.isVulnerable,
                            confidence = result.confidence,
                            evidence = result.response.take(100)
                        )
                    )
                )

            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message ?: "Test failed")
            } finally {
                _isTesting.value = false
            }
        }
    }

    fun clearUrl() {
        _url.value = ""
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    data class ManualTestState(
        val url: String = "",
        val isTesting: Boolean = false,
        val isVulnerable: Boolean? = null,
        val confidence: com.yourapp.sqliautohunter.domain.model.ConfidenceLevel? = null,
        val vulnerabilityTypes: List<com.yourapp.sqliautohunter.domain.model.VulnerabilityType> = emptyList(),
        val detectionResults: List<ScanUrlForVulnerabilityUseCase.DetectionResult> = emptyList(),
        val isAlreadyTested: Boolean = false,
        val previousResult: String? = null,
        val error: String? = null
    )
}
