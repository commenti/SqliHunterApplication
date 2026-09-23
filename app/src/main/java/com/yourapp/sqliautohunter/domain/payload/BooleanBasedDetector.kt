package com.yourapp.sqliautohunter.domain.payload

import com.yourapp.sqliautohunter.domain.payload.SqliPayloadTemplates.PayloadType
import com.yourapp.sqliautohunter.util.HashUtils

class BooleanBasedDetector {

    companion object {
        private const val CONTENT_LENGTH_TOLERANCE = 10
        private const val HASH_MATCH_THRESHOLD = 0.95
    }

    data class BooleanTestResult(
        val isVulnerable: Boolean,
        val confidence: Double,
        val trueResponseLength: Int,
        val falseResponseLength: Int,
        val trueResponseHash: String,
        val falseResponseHash: String,
        val lengthDifference: Int,
        val hashSimilarity: Double
    )

    fun test(url: String, truePayload: String, falsePayload: String): BooleanTestResult {
        // In a real implementation, this would make HTTP requests
        // For now, we'll simulate the comparison logic
        
        // Simulate responses (in actual use, these would come from HTTP calls)
        val trueResponse = simulateResponse(url, truePayload)
        val falseResponse = simulateResponse(url, falsePayload)
        
        return compareResponses(trueResponse, falseResponse)
    }

    fun testWithActualResponses(trueResponse: String, falseResponse: String): BooleanTestResult {
        return compareResponses(trueResponse, falseResponse)
    }

    private fun compareResponses(trueResponse: String, falseResponse: String): BooleanTestResult {
        val trueLength = trueResponse.length
        val falseLength = falseResponse.length
        val lengthDifference = kotlin.math.abs(trueLength - falseLength)
        
        val trueHash = HashUtils.sha256(trueResponse)
        val falseHash = HashUtils.sha256(falseResponse)
        
        val hashSimilarity = calculateHashSimilarity(trueHash, falseHash)
        
        // Determine if vulnerable based on differences
        val lengthDiffSignificant = lengthDifference > CONTENT_LENGTH_TOLERANCE
        val hashDiffSignificant = hashSimilarity < HASH_MATCH_THRESHOLD
        
        val isVulnerable = lengthDiffSignificant || hashDiffSignificant
        val confidence = if (isVulnerable) {
            if (lengthDiffSignificant && hashDiffSignificant) 0.95
            else if (lengthDiffSignificant || hashDiffSignificant) 0.80
            else 0.50
        } else {
            0.0
        }
        
        return BooleanTestResult(
            isVulnerable = isVulnerable,
            confidence = confidence,
            trueResponseLength = trueLength,
            falseResponseLength = falseLength,
            trueResponseHash = trueHash,
            falseResponseHash = falseHash,
            lengthDifference = lengthDifference,
            hashSimilarity = hashSimilarity
        )
    }

    private fun calculateHashSimilarity(hash1: String, hash2: String): Double {
        if (hash1.length != hash2.length || hash1.isEmpty()) return 0.0
        
        var matchingChars = 0
        for (i in hash1.indices) {
            if (hash1[i] == hash2[i]) {
                matchingChars++
            }
        }
        
        return matchingChars.toDouble() / hash1.length.toDouble()
    }

    private fun simulateResponse(url: String, payload: String): String {
        // This is a simulation - in real implementation, this would make HTTP requests
        // For testing purposes, we'll return different content based on payload
        return if (payload.contains("1=1")) {
            "HTML content for true condition"
        } else {
            "Different HTML content for false condition"
        }
    }

    fun testMultiplePayloads(
        url: String,
        truePayloads: List<String> = SqliPayloadTemplates.BOOLEAN_BASED_PAYLOADS.filter { it.contains("1=1") },
        falsePayloads: List<String> = SqliPayloadTemplates.BOOLEAN_BASED_PAYLOADS.filter { it.contains("1=2") }
    ): List<BooleanTestResult> {
        val results = mutableListOf<BooleanTestResult>()
        
        val minCount = minOf(truePayloads.size, falsePayloads.size)
        
        for (i in 0 until minCount) {
            val result = test(url, truePayloads[i], falsePayloads[i])
            results.add(result)
        }
        
        return results
    }

    fun aggregateResults(results: List<BooleanTestResult>): AggregatedBooleanResult {
        val vulnerableCount = results.count { it.isVulnerable }
        val totalCount = results.size
        
        val avgConfidence = if (totalCount > 0) {
            results.map { it.confidence }.average()
        } else {
            0.0
        }
        
        val avgLengthDiff = if (totalCount > 0) {
            results.map { it.lengthDifference }.average()
        } else {
            0.0
        }
        
        val avgHashSimilarity = if (totalCount > 0) {
            results.map { it.hashSimilarity }.average()
        } else {
            1.0
        }
        
        return AggregatedBooleanResult(
            isVulnerable = vulnerableCount > totalCount / 2,
            confidence = if (totalCount > 0) vulnerableCount.toDouble() / totalCount else 0.0,
            testCount = totalCount,
            positiveCount = vulnerableCount,
            averageConfidence = avgConfidence,
            averageLengthDifference = avgLengthDiff,
            averageHashSimilarity = avgHashSimilarity
        )
    }

    data class AggregatedBooleanResult(
        val isVulnerable: Boolean,
        val confidence: Double,
        val testCount: Int,
        val positiveCount: Int,
        val averageConfidence: Double,
        val averageLengthDifference: Double,
        val averageHashSimilarity: Double
    )
}
