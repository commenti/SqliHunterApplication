package com.yourapp.sqliautohunter.engine.browser

import android.webkit.WebView
import com.yourapp.sqliautohunter.domain.payload.BooleanBasedDetector
import com.yourapp.sqliautohunter.domain.payload.ErrorBasedDetector
import com.yourapp.sqliautohunter.domain.payload.SqliPayloadTemplates
import com.yourapp.sqliautohunter.domain.payload.TimeBasedDetector
import com.yourapp.sqliautohunter.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class PayloadInjector(
    private val cdpSessionController: CdpSessionController
) {

    private val errorDetector = ErrorBasedDetector()
    private val booleanDetector = BooleanBasedDetector()
    private val timeDetector = TimeBasedDetector()

    private val MAX_RETRIES = 3
    private val INJECTION_TIMEOUT_MS = 30000L

    data class InjectionResult(
        val isVulnerable: Boolean,
        val vulnerabilityType: String,
        val confidence: Double,
        val payload: String,
        val response: String,
        val responseTimeMs: Long,
        val error: String? = null
    )

    suspend fun injectAndTest(
        url: String,
        payloads: List<String> = SqliPayloadTemplates.ALL_PAYLOADS
    ): List<InjectionResult> {
        return withContext(Dispatchers.IO) {
            val results = mutableListOf<InjectionResult>()
            
            for (payload in payloads.take(10)) { // Limit to 10 payloads for performance
                val result = tryInjectPayload(url, payload)
                results.add(result)
                
                // Add delay between injections
                delay(500)
            }
            
            results
        }
    }

    suspend fun tryInjectPayload(url: String, payload: String): InjectionResult {
        return withContext(Dispatchers.IO) {
            var retries = 0
            
            while (retries < MAX_RETRIES) {
                try {
                    // Create session and navigate to URL
                    val sessionCreated = cdpSessionController.createSession(url)
                    if (!sessionCreated) {
                        return@withContext InjectionResult(
                            isVulnerable = false,
                            vulnerabilityType = "unknown",
                            confidence = 0.0,
                            payload = payload,
                            response = "",
                            responseTimeMs = 0,
                            error = "Failed to create session"
                        )
                    }
                    
                    // Inject payload
                    val injected = cdpSessionController.injectPayload(payload)
                    if (!injected) {
                        return@withContext InjectionResult(
                            isVulnerable = false,
                            vulnerabilityType = "unknown",
                            confidence = 0.0,
                            payload = payload,
                            response = "",
                            responseTimeMs = 0,
                            error = "Failed to inject payload"
                        )
                    }
                    
                    // Wait for response
                    delay(2000) // Wait for page to process
                    
                    // Capture response
                    val responseData = cdpSessionController.captureResponse(url, payload)
                    
                    if (responseData == null) {
                        retries++
                        continue
                    }
                    
                    // Analyze response
                    val analysisResult = analyzeResponse(
                        url = responseData.url,
                        content = responseData.content,
                        responseTimeMs = responseData.responseTimeMs,
                        payload = payload
                    )
                    
                    cdpSessionController.closeSession()
                    
                    return@withContext InjectionResult(
                        isVulnerable = analysisResult.isVulnerable,
                        vulnerabilityType = analysisResult.vulnerabilityType.name,
                        confidence = analysisResult.confidence,
                        payload = payload,
                        response = responseData.content,
                        responseTimeMs = responseData.responseTimeMs,
                        error = null
                    )
                    
                } catch (e: Exception) {
                    retries++
                    if (retries >= MAX_RETRIES) {
                        return@withContext InjectionResult(
                            isVulnerable = false,
                            vulnerabilityType = "unknown",
                            confidence = 0.0,
                            payload = payload,
                            response = "",
                            responseTimeMs = 0,
                            error = e.message
                        )
                    }
                    delay(1000 * retries.toLong())
                }
            }
            
            // Should not reach here
            InjectionResult(
                isVulnerable = false,
                vulnerabilityType = "unknown",
                confidence = 0.0,
                payload = payload,
                response = "",
                responseTimeMs = 0,
                error = "Max retries exceeded"
            )
        }
    }

    private fun analyzeResponse(
        url: String,
        content: String,
        responseTimeMs: Long,
        payload: String
    ): AnalysisResult {
        val payloadType = SqliPayloadTemplates.getPayloadType(payload)
        
        return when (payloadType) {
            SqliPayloadTemplates.PayloadType.ERROR_BASED -> {
                val detectionResult = errorDetector.detect(content)
                AnalysisResult(
                    isVulnerable = detectionResult.isVulnerable,
                    vulnerabilityType = com.yourapp.sqliautohunter.domain.model.VulnerabilityType.ERROR_BASED,
                    confidence = detectionResult.confidence
                )
            }
            SqliPayloadTemplates.PayloadType.BOOLEAN_BASED -> {
                // For boolean-based, we'd need to compare with a non-vulnerable response
                // This is simplified for now
                AnalysisResult(
                    isVulnerable = false,
                    vulnerabilityType = com.yourapp.sqliautohunter.domain.model.VulnerabilityType.BOOLEAN_BASED,
                    confidence = 0.5
                )
            }
            SqliPayloadTemplates.PayloadType.TIME_BASED -> {
                val isSlow = responseTimeMs >= Constants.SLEEP_TEST_DELAY_MS + Constants.SLEEP_TEST_TOLERANCE_MS
                AnalysisResult(
                    isVulnerable = isSlow,
                    vulnerabilityType = com.yourapp.sqliautohunter.domain.model.VulnerabilityType.TIME_BASED,
                    confidence = if (isSlow) 0.9 else 0.0
                )
            }
            SqliPayloadTemplates.PayloadType.UNION_BASED -> {
                val detectionResult = errorDetector.detect(content)
                AnalysisResult(
                    isVulnerable = detectionResult.isVulnerable,
                    vulnerabilityType = com.yourapp.sqliautohunter.domain.model.VulnerabilityType.UNION_BASED,
                    confidence = detectionResult.confidence
                )
            }
            else -> {
                val detectionResult = errorDetector.detect(content)
                AnalysisResult(
                    isVulnerable = detectionResult.isVulnerable,
                    vulnerabilityType = com.yourapp.sqliautohunter.domain.model.VulnerabilityType.ERROR_BASED,
                    confidence = detectionResult.confidence
                )
            }
        }
    }

    private data class AnalysisResult(
        val isVulnerable: Boolean,
        val vulnerabilityType: com.yourapp.sqliautohunter.domain.model.VulnerabilityType,
        val confidence: Double
    )

    suspend fun testWithAllTechniques(url: String): List<InjectionResult> {
        return withContext(Dispatchers.IO) {
            val results = mutableListOf<InjectionResult>()
            
            // Error-based test
            val errorPayloads = SqliPayloadTemplates.ERROR_BASED_PAYLOADS.take(3)
            for (payload in errorPayloads) {
                results.add(tryInjectPayload(url, payload))
            }
            
            // Boolean-based test
            val truePayload = SqliPayloadTemplates.BOOLEAN_BASED_PAYLOADS.first { it.contains("1=1") }
            val falsePayload = SqliPayloadTemplates.BOOLEAN_BASED_PAYLOADS.first { it.contains("1=2") }
            
            // For boolean-based, we need to compare responses
            val trueResult = tryInjectPayload(url, truePayload)
            val falseResult = tryInjectPayload(url, falsePayload)
            
            // Analyze boolean-based
            if (trueResult.response.isNotEmpty() && falseResult.response.isNotEmpty()) {
                val booleanResult = booleanDetector.testWithActualResponses(
                    trueResult.response,
                    falseResult.response
                )
                
                results.add(InjectionResult(
                    isVulnerable = booleanResult.isVulnerable,
                    vulnerabilityType = "BOOLEAN_BASED",
                    confidence = booleanResult.confidence,
                    payload = "$truePayload vs $falsePayload",
                    response = trueResult.response,
                    responseTimeMs = trueResult.responseTimeMs,
                    error = null
                ))
            }
            
            // Time-based test
            val timePayload = SqliPayloadTemplates.TIME_BASED_PAYLOADS.first()
            val timeResult = tryInjectPayload(url, timePayload)
            results.add(timeResult)
            
            results
        }
    }

    suspend fun quickTest(url: String): InjectionResult {
        return withContext(Dispatchers.IO) {
            // Use the most reliable error-based payload
            val payload = SqliPayloadTemplates.ERROR_BASED_PAYLOADS.first()
            tryInjectPayload(url, payload)
        }
    }
}
