package com.yourapp.sqliautohunter.domain.payload

import com.yourapp.sqliautohunter.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.system.measureTimeMillis

class TimeBasedDetector {

    companion object {
        private const val DEFAULT_SLEEP_TIME_MS = 5000L
        private const val TOLERANCE_MS = 2000L
        private const val MIN_CONFIDENCE_SLEEP_TIME = 3000L
        private const val MAX_CONFIDENCE_SLEEP_TIME = 7000L
    }

    data class TimeTestResult(
        val isVulnerable: Boolean,
        val confidence: Double,
        val responseTimeMs: Long,
        val expectedDelayMs: Long,
        val timeDifferenceMs: Long,
        val isWithinTolerance: Boolean
    )

    suspend fun test(
        url: String,
        payload: String = Constants.Payloads.TIME_BASED_SLEEP,
        expectedDelayMs: Long = DEFAULT_SLEEP_TIME_MS
    ): TimeTestResult {
        return withContext(Dispatchers.IO) {
            val responseTime = measureTimeMillis {
                // Simulate HTTP request with payload
                // In real implementation, this would make an actual HTTP call
                simulateDelayedRequest(url, payload, expectedDelayMs)
            }
            
            val timeDifference = responseTime - expectedDelayMs
            val isWithinTolerance = timeDifference >= -TOLERANCE_MS && timeDifference <= TOLERANCE_MS
            
            // If response took significantly longer than expected, it's likely vulnerable
            val isVulnerable = responseTime >= expectedDelayMs + TOLERANCE_MS
            
            val confidence = calculateConfidence(responseTime, expectedDelayMs, isVulnerable)
            
            TimeTestResult(
                isVulnerable = isVulnerable,
                confidence = confidence,
                responseTimeMs = responseTime,
                expectedDelayMs = expectedDelayMs,
                timeDifferenceMs = timeDifference,
                isWithinTolerance = isWithinTolerance
            )
        }
    }

    suspend fun testWithActualRequest(
        requestFunction: suspend () -> Unit,
        expectedDelayMs: Long = DEFAULT_SLEEP_TIME_MS
    ): TimeTestResult {
        return withContext(Dispatchers.IO) {
            val responseTime = measureTimeMillis {
                requestFunction()
            }
            
            val timeDifference = responseTime - expectedDelayMs
            val isWithinTolerance = timeDifference >= -TOLERANCE_MS && timeDifference <= TOLERANCE_MS
            val isVulnerable = responseTime >= expectedDelayMs + TOLERANCE_MS
            
            val confidence = calculateConfidence(responseTime, expectedDelayMs, isVulnerable)
            
            TimeTestResult(
                isVulnerable = isVulnerable,
                confidence = confidence,
                responseTimeMs = responseTime,
                expectedDelayMs = expectedDelayMs,
                timeDifferenceMs = timeDifference,
                isWithinTolerance = isWithinTolerance
            )
        }
    }

    private fun calculateConfidence(
        responseTime: Long,
        expectedDelay: Long,
        isVulnerable: Boolean
    ): Double {
        return if (!isVulnerable) {
            0.0
        } else {
            // Higher confidence for responses that are closer to expected delay
            val timeRatio = responseTime.toDouble() / expectedDelay.toDouble()
            
            when {
                timeRatio >= 2.0 -> 0.95  // Response took at least 2x expected time
                timeRatio >= 1.5 -> 0.85
                timeRatio >= 1.2 -> 0.70
                timeRatio >= 1.0 -> 0.50
                else -> 0.30
            }
        }
    }

    private suspend fun simulateDelayedRequest(url: String, payload: String, delayMs: Long) {
        // Simulate a delayed response for testing
        // In real implementation, this would be an actual HTTP request
        if (payload.contains("SLEEP") || payload.contains("sleep")) {
            withContext(Dispatchers.IO) {
                kotlin.runCatching {
                    Thread.sleep(delayMs)
                }
            }
        } else {
            // Normal request without delay
            withContext(Dispatchers.IO) {
                Thread.sleep(100) // Simulate network latency
            }
        }
    }

    suspend fun testMultiplePayloads(
        url: String,
        payloads: List<String> = SqliPayloadTemplates.TIME_BASED_PAYLOADS,
        expectedDelayMs: Long = DEFAULT_SLEEP_TIME_MS
    ): List<TimeTestResult> {
        return payloads.map { payload ->
            test(url, payload, expectedDelayMs)
        }
    }

    fun aggregateResults(results: List<TimeTestResult>): AggregatedTimeResult {
        val vulnerableCount = results.count { it.isVulnerable }
        val totalCount = results.size
        
        val avgConfidence = if (totalCount > 0) {
            results.map { it.confidence }.average()
        } else {
            0.0
        }
        
        val avgResponseTime = if (totalCount > 0) {
            results.map { it.responseTimeMs }.average().toLong()
        } else {
            0L
        }
        
        val avgTimeDifference = if (totalCount > 0) {
            results.map { it.timeDifferenceMs }.average().toLong()
        } else {
            0L
        }
        
        return AggregatedTimeResult(
            isVulnerable = vulnerableCount > totalCount / 2,
            confidence = if (totalCount > 0) vulnerableCount.toDouble() / totalCount else 0.0,
            testCount = totalCount,
            positiveCount = vulnerableCount,
            averageConfidence = avgConfidence,
            averageResponseTimeMs = avgResponseTime,
            averageTimeDifferenceMs = avgTimeDifference
        )
    }

    suspend fun verifyWithDoubleTest(
        url: String,
        payload: String,
        expectedDelayMs: Long = DEFAULT_SLEEP_TIME_MS
    ): VerifiedTimeResult {
        // First test with the time-based payload
        val firstResult = test(url, payload, expectedDelayMs)
        
        // Second test with a non-time-based payload (should be fast)
        val nonTimePayload = SqliPayloadTemplates.ERROR_BASED_PAYLOADS.first()
        val secondResponseTime = measureTimeMillis {
            simulateDelayedRequest(url, nonTimePayload, 0)
        }
        
        // If first test was slow and second was fast, it's likely vulnerable
        val timeDifference = firstResult.responseTimeMs - secondResponseTime
        val isVulnerable = firstResult.isVulnerable && timeDifference >= expectedDelayMs - TOLERANCE_MS
        
        return VerifiedTimeResult(
            isVulnerable = isVulnerable,
            confidence = if (isVulnerable) firstResult.confidence * 0.9 else 0.0,
            firstTest = firstResult,
            secondResponseTimeMs = secondResponseTime,
            timeDifferenceMs = timeDifference
        )
    }

    data class AggregatedTimeResult(
        val isVulnerable: Boolean,
        val confidence: Double,
        val testCount: Int,
        val positiveCount: Int,
        val averageConfidence: Double,
        val averageResponseTimeMs: Long,
        val averageTimeDifferenceMs: Long
    )

    data class VerifiedTimeResult(
        val isVulnerable: Boolean,
        val confidence: Double,
        val firstTest: TimeTestResult,
        val secondResponseTimeMs: Long,
        val timeDifferenceMs: Long
    )
}
