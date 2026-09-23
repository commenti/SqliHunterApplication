package com.yourapp.sqliautohunter.domain.model

/**
 * Lifecycle state of a ScanTarget / search_queue row.
 *
 * `wire` matches SearchQueueEntity.STATUS_* exactly — the DB persists the
 * string, not the ordinal, so reordering this enum is safe. `isTerminal`
 * flags states where no further scanning is expected unless the user retries.
 *
 * Transitions (enforced by callers, not by this enum):
 *   PENDING -> TESTING -> VULNERABLE | NOT_VULNERABLE | ERROR
 *   TESTING -> PENDING (requeue on service restart)
 *   ERROR   -> PENDING (manual retry)
 */
enum class ScanStatus(val wire: String, val label: String) {
    PENDING("pending", "Pending"),
    TESTING("testing", "Testing"),
    VULNERABLE("vulnerable", "Vulnerable"),
    NOT_VULNERABLE("not_vulnerable", "Not vulnerable"),
    ERROR("error", "Error");

    val isTerminal: Boolean
        get() = this == VULNERABLE || this == NOT_VULNERABLE || this == ERROR

    val isActive: Boolean
        get() = this == PENDING || this == TESTING

    companion object {
        fun fromWire(wire: String): ScanStatus =
            entries.firstOrNull { it.wire == wire } ?: PENDING

        /** Statuses that should be reset to PENDING on cold start. */
        fun stuckOnRestart(): List<ScanStatus> = listOf(TESTING)

        /** Statuses the UI shows as "in flight". */
        fun inFlight(): List<ScanStatus> = listOf(PENDING, TESTING)

        /** Statuses eligible for the purge-queue action. */
        fun purgeable(): List<ScanStatus> = listOf(NOT_VULNERABLE, ERROR)
    }
}