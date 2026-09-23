package com.yourapp.sqliautohunter.domain.usecase

import com.yourapp.sqliautohunter.domain.model.ConfidenceLevel
import com.yourapp.sqliautohunter.domain.model.VulnerabilityType
import com.yourapp.sqliautohunter.domain.payload.ErrorBasedDetector
import com.yourapp.sqliautohunter.domain.payload.SqliPayloadTemplates

class ClassifyResultUseCase {

    private val errorDetector = ErrorBasedDetector()

    operator fun invoke(
        responseBody: String,
        responseHeaders: Map<String, String> = emptyMap(),
        payload: String,
        responseTimeMs: Long? = null
    ): ClassificationResult {
        val detectionResult = errorDetector.detect(responseBody, responseHeaders)
        val payloadType = SqliPayloadTemplates.getPayloadType(payload)
        
        val vulnerabilityType = when {
            detectionResult.isVulnerable -> VulnerabilityType.ERROR_BASED
            payloadType == SqliPayloadTemplates.PayloadType.BOOLEAN_BASED -> VulnerabilityType.BOOLEAN_BASED
            payloadType == SqliPayloadTemplates.PayloadType.TIME_BASED -> {
                if (responseTimeMs != null && isTimeBasedVulnerable(responseTimeMs)) {
                    VulnerabilityType.TIME_BASED
                } else {
                    VulnerabilityType.ERROR_BASED
                }
            }
            payloadType == SqliPayloadTemplates.PayloadType.UNION_BASED -> VulnerabilityType.UNION_BASED
            else -> VulnerabilityType.ERROR_BASED
        }

        val confidence = calculateConfidence(
            detectionResult = detectionResult,
            payloadType = payloadType,
            responseTimeMs = responseTimeMs
        )

        return ClassificationResult(
            vulnerabilityType = vulnerabilityType,
            confidenceLevel = confidence,
            isVulnerable = detectionResult.isVulnerable || (payloadType == SqliPayloadTemplates.PayloadType.TIME_BASED && isTimeBasedVulnerable(responseTimeMs)),
            evidence = detectionResult.evidence.ifEmpty { "Time delay detected" },
            payloadUsed = payload
        )
    }

    private fun isTimeBasedVulnerable(responseTimeMs: Long?): Boolean {
        if (responseTimeMs == null) return false
        return responseTimeMs >= Constants.SLEEP_TEST_DELAY_MS + Constants.SLEEP_TEST_TOLERANCE_MS
    }

    private fun calculateConfidence(
        detectionResult: ErrorBasedDetector.DetectionResult,
        payloadType: SqliPayloadTemplates.PayloadType,
        responseTimeMs: Long?
    ): ConfidenceLevel {
        return when {
            // High confidence: multiple indicators
            detectionResult.isVulnerable && detectionResult.confidence >= 0.8 -> ConfidenceLevel.HIGH
            payloadType == SqliPayloadTemplates.PayloadType.TIME_BASED && isTimeBasedVulnerable(responseTimeMs) -> ConfidenceLevel.HIGH
            
            // Medium confidence: single indicator
            detectionResult.isVulnerable && detectionResult.confidence >= 0.5 -> ConfidenceLevel.MEDIUM
            payloadType == SqliPayloadTemplates.PayloadType.BOOLEAN_BASED && detectionResult.isVulnerable -> ConfidenceLevel.MEDIUM
            
            // Low confidence: weak signal
            detectionResult.isVulnerable -> ConfidenceLevel.LOW
            
            else -> ConfidenceLevel.LOW
        }
    }

    fun classifyBatch(
        results: List<Pair<String, Map<String, String>>>,
        payloads: List<String>
    ): List<ClassificationResult> {
        return results.mapIndexed { index, (body, headers) ->
            val payload = payloads.getOrElse(index) { "" }
            invoke(body, headers, payload)
        }
    }

    fun aggregateClassifications(results: List<ClassificationResult>): AggregatedClassification {
        val vulnerableCount = results.count { it.isVulnerable }
        val totalCount = results.size
        
        val typeCounts = mutableMapOf<VulnerabilityType, Int>()
        val confidenceCounts = mutableMapOf<ConfidenceLevel, Int>()
        
        results.forEach { result ->
            if (result.isVulnerable) {
                typeCounts[result.vulnerabilityType] = typeCounts.getOrDefault(result.vulnerabilityType, 0) + 1
                confidenceCounts[result.confidenceLevel] = confidenceCounts.getOrDefault(result.confidenceLevel, 0) + 1
            }
        }
        
        val primaryType = typeCounts.maxByOrNull { it.value }?.key ?: VulnerabilityType.ERROR_BASED
        val primaryConfidence = confidenceCounts.maxByOrNull { it.value }?.key ?: ConfidenceLevel.LOW
        
        return AggregatedClassification(
            totalTests = totalCount,
            vulnerableCount = vulnerableCount,
            primaryVulnerabilityType = primaryType,
            primaryConfidenceLevel = primaryConfidence,
            typeDistribution = typeCounts,
            confidenceDistribution = confidenceCounts,
            isVulnerable = vulnerableCount > totalCount / 2
        )
    }

    data class ClassificationResult(
        val vulnerabilityType: VulnerabilityType,
        val confidenceLevel: ConfidenceLevel,
        val isVulnerable: Boolean,
        val evidence: String,
        val payloadUsed: String
    )

    data class AggregatedClassification(
        val totalTests: Int,
        val vulnerableCount: Int,
        val primaryVulnerabilityType: VulnerabilityType,
        val primaryConfidenceLevel: ConfidenceLevel,
        val typeDistribution: Map<VulnerabilityType, Int>,
        val confidenceDistribution: Map<ConfidenceLevel, Int>,
        val isVulnerable: Boolean
    )
}
