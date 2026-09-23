package com.yourapp.sqliautohunter.domain.usecase

import com.yourapp.sqliautohunter.domain.model.ScanTarget
import com.yourapp.sqliautohunter.util.HashUtils
import com.yourapp.sqliautohunter.util.UrlNormalizer
import javax.inject.Inject

/**
 * Normalize a raw URL and attach the canonical form + SHA-256 hash to a
 * ScanTarget.
 *
 * Wraps UrlNormalizer + HashUtils so the rest of the pipeline depends on the
 * domain model and never touches the util layer directly — swaps, fakes, and
 * tests all stay in one place.
 *
 * Pure and synchronous. Callers dispatch on their own dispatcher.
 *
 * Failure modes:
 *   - Null / blank / oversized input -> Outcome.Rejected with a reason.
 *   - Non-http(s) scheme              -> Outcome.Rejected("scheme").
 *   - Unparseable URI                 -> Outcome.Rejected("malformed").
 *
 * On success the returned ScanTarget has both `normalizedUrl` and `urlHash`
 * populated and `isReady == true`.
 */
class NormalizeUrlUseCase @Inject constructor() {

    sealed interface Outcome {
        data class Ok(val target: ScanTarget) : Outcome
        data class Rejected(val rawUrl: String, val reason: String) : Outcome
    }

    operator fun invoke(target: ScanTarget): Outcome = invoke(target.url, target.keywordSource)

    operator fun invoke(rawUrl: String, keywordSource: String): Outcome {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) return Outcome.Rejected(rawUrl, "blank")

        val parsed = UrlNormalizer.parse(trimmed)
            ?: return Outcome.Rejected(rawUrl, rejectReason(trimmed))

        val canonical = parsed.normalized
        val hash = HashUtils.hashUrl(canonical)

        val target = ScanTarget(
            url = trimmed,
            normalizedUrl = canonical,
            urlHash = hash,
            keywordSource = keywordSource,
            status = com.yourapp.sqliautohunter.domain.model.ScanStatus.PENDING,
            discoveredAt = System.currentTimeMillis()
        )
        return Outcome.Ok(target)
    }

    /**
     * Batch variant. Rejects are dropped into a separate list so the caller can
     * route them to the error bucket without aborting the whole batch.
     */
    fun batch(
        rawUrls: List<String>,
        keywordSource: String
    ): BatchResult {
        val accepted = ArrayList<ScanTarget>(rawUrls.size)
        val rejected = ArrayList<Outcome.Rejected>()
        for (raw in rawUrls) {
            when (val out = invoke(raw, keywordSource)) {
                is Outcome.Ok -> accepted += out.target
                is Outcome.Rejected -> rejected += out
            }
        }
        return BatchResult(accepted, rejected)
    }

    data class BatchResult(
        val accepted: List<ScanTarget>,
        val rejected: List<Outcome.Rejected>
    )

    private fun rejectReason(url: String): String = when {
        url.length > com.yourapp.sqliautohunter.util.Constants.URL_MAX_LEN -> "oversized"
        !url.startsWith("http://", true) && !url.startsWith("https://", true) &&
            url.contains("://") -> "scheme"
        else -> "malformed"
    }
}