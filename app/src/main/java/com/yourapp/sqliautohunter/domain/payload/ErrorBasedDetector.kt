package com.yourapp.sqliautohunter.domain.payload

import com.yourapp.sqliautohunter.util.Constants

class ErrorBasedDetector {

    private val errorSignatures = Constants.SQL_ERROR_SIGNATURES

    fun detect(responseBody: String, responseHeaders: Map<String, String> = emptyMap()): DetectionResult {
        val normalizedBody = responseBody.lowercase()
        
        // Check body for SQL error signatures
        val bodyMatches = errorSignatures.filter { signature ->
            normalizedBody.contains(signature.lowercase())
        }

        // Check headers for SQL error signatures
        val headerMatches = responseHeaders.entries.filter { (_, value) ->
            errorSignatures.any { signature ->
                value.lowercase().contains(signature.lowercase())
            }
        }.map { it.value }

        val allMatches = bodyMatches + headerMatches
        
        return if (allMatches.isNotEmpty()) {
            DetectionResult(
                isVulnerable = true,
                confidence = calculateConfidence(allMatches.size),
                evidence = allMatches.joinToString(", "),
                signature = allMatches.first()
            )
        } else {
            DetectionResult(
                isVulnerable = false,
                confidence = 0.0,
                evidence = "",
                signature = ""
            )
        }
    }

    fun detectBatch(responses: List<Pair<String, Map<String, String>>>): List<DetectionResult> {
        return responses.map { (body, headers) ->
            detect(body, headers)
        }
    }

    private fun calculateConfidence(matchCount: Int): Double {
        return when {
            matchCount >= 5 -> 0.95
            matchCount >= 3 -> 0.85
            matchCount >= 2 -> 0.70
            matchCount >= 1 -> 0.50
            else -> 0.0
        }
    }

    fun isSqlErrorPage(responseBody: String): Boolean {
        val normalized = responseBody.lowercase()
        return errorSignatures.any { signature ->
            normalized.contains(signature.lowercase())
        }
    }

    data class DetectionResult(
        val isVulnerable: Boolean,
        val confidence: Double,
        val evidence: String,
        val signature: String
    )
}
