package com.yourapp.sqliautohunter.domain.model

/**
 * Confidence tier for a confirmed vulnerability.
 *
 * Derived by ClassifyResultUseCase from the number and weight of independent
 * techniques that confirmed the same parameter. `wire` is what gets persisted
 * (see VulnerabilityResultEntity.CONFIDENCE_*) and exported to CSV.
 *
 * Scoring contract (see ConfidenceLevel.fromScore):
 *   - score >= HIGH_THRESHOLD  -> HIGH
 *   - score >= MED_THRESHOLD   -> MEDIUM
 *   - score >  0               -> LOW
 *   - score == 0               -> NONE
 */
enum class ConfidenceLevel(val wire: String, val label: String, val rank: Int) {
    HIGH("High", "High", 3),
    MEDIUM("Medium", "Medium", 2),
    LOW("Low", "Low", 1),
    NONE("None", "None", 0);

    /** True when this level is strong enough to persist as a result row. */
    val isReportable: Boolean
        get() = this != NONE

    companion object {
        const val HIGH_THRESHOLD = 8
        const val MED_THRESHOLD = 5

        fun fromWire(wire: String): ConfidenceLevel =
            entries.firstOrNull { it.wire.equals(wire, ignoreCase = true) } ?: NONE

        /** Map a raw score (sum of VulnerabilityType.baseWeight per confirm) to a tier. */
        fun fromScore(score: Int): ConfidenceLevel = when {
            score <= 0 -> NONE
            score >= HIGH_THRESHOLD -> HIGH
            score >= MED_THRESHOLD -> MEDIUM
            else -> LOW
        }
    }
}