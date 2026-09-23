package com.yourapp.sqliautohunter.domain.model

/**
 * The unit of work flowing through the pipeline.
 *
 * Created by the search scraper (or the manual-URL screen), normalized and
 * hashed by the dedup gate, then queued and dispatched to a WebView worker.
 *
 * `normalizedUrl` is authoritative once set — the raw `url` is kept for logging
 * and for re-queuing if normalization was rejected.
 */
data class ScanTarget(
    val url: String,
    val normalizedUrl: String?,
    val urlHash: String?,
    val keywordSource: String,
    val status: ScanStatus = ScanStatus.PENDING,
    val discoveredAt: Long = System.currentTimeMillis(),
    val enqueuedAt: Long? = null,
    val attemptedAt: Long? = null,
    val attemptCount: Int = 0
) {
    /** True when the target has been normalized + hashed and is ready to scan. */
    val isReady: Boolean
        get() = normalizedUrl != null && urlHash != null

    /**
     * Copy with a bumped attempt counter and a fresh attemptedAt timestamp.
     * Used by the retry loop — never mutates in place.
     */
    fun withAttempt(): ScanTarget = copy(
        attemptCount = attemptCount + 1,
        attemptedAt = System.currentTimeMillis()
    )

    /** Copy with a new status. */
    fun withStatus(newStatus: ScanStatus): ScanTarget = copy(status = newStatus)

    companion object {
        /**
         * Build a target from raw scraper output. Normalization + hashing are
         * performed by the dedup use case, not here — this keeps the domain
         * model dependency-free.
         */
        fun fromRaw(url: String, keywordSource: String): ScanTarget = ScanTarget(
            url = url,
            normalizedUrl = null,
            urlHash = null,
            keywordSource = keywordSource,
            status = ScanStatus.PENDING,
            discoveredAt = System.currentTimeMillis()
        )
    }
}