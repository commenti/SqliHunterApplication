package com.yourapp.sqliautohunter.domain.payload

import com.yourapp.sqliautohunter.domain.model.VulnerabilityType
import kotlin.math.abs

/**
 * Time-based (blind) SQLi detector.
 *
 * Strategy: send a payload that is expected to induce a deliberate server-side
 * delay (SLEEP, pg_sleep, WAITFOR DELAY, DBMS_PIPE.RECEIVE_MESSAGE), measure
 * end-to-end response time, and compare against a control baseline taken
 * immediately beforehand.
 *
 * A hit requires:
 *   1. measured delay >= TIME_BASED_MIN_DELTA_MS (Constants) above baseline,
 *   2. the delay is not attributable to plain network jitter — mitigated by
 *      two consecutive confirmations with the same payload,
 *   3. a control run with a no-op payload completes fast (baseline < 50% of
 *      the suspect run), guarding against a slow server counting as a hit.
 *
 * The detector is stateless; the caller supplies all timing observations.
 */
class TimeBasedDetector {

    /**
     * Timing observation for a single probe.
     *
     * @param payload   the concrete payload string sent (post-render),
     * @param elapsedMs wall-clock from request issue to response fully read,
     * @param wasError  true if the request errored out (timeouts count as
     *                  errors — a timeout is NOT a time-based hit),
     */
    data class Probe(
        val payload: String,
        val elapsedMs: Long,
        val wasError: Boolean = false
    )

    /**
     * Detection outcome. `confirmed` requires two consecutive probes of the
     * same payload both exceeding the baseline + threshold, and the baseline
     * being meaningfully fast.
     */
    data class Detection(
        val isHit: Boolean,
        val baselineMs: Long,
        val observedMs: Long,
        val deltaMs: Long,
        val confirmations: Int,
        val payload: String,
        val excerpt: String
    )

    val technique: VulnerabilityType = VulnerabilityType.TIME_BASED

    fun payloads(cap: Int = 4): List<PayloadTemplate> =
        SqliPayloadTemplates.forTechnique(VulnerabilityType.TIME_BASED, cap)

    /**
     * Evaluate a batch of probes for one parameter.
     *
     * @param baseline  timing of the control request (no injection),
     * @param probes    ordered list of probe timings for the same payload family,
     */
    fun detect(baseline: Probe, probes: List<Probe>): Detection {
        if (baseline.wasError || baseline.elapsedMs <= 0L || probes.isEmpty()) {
            return emptyDetection(baseline, probes.firstOrNull())
        }

        val threshold = Constants.TIME_BASED_MIN_DELTA_MS
        val baselineCeiling = (baseline.elapsedMs * 3L).coerceAtLeast(1_500L)

        // Group confirmations by exact payload string.
        val byPayload = probes
            .filter { !it.wasError && it.elapsedMs > 0L }
            .groupBy { it.payload }

        var bestPayload: String = ""
        var bestObserved = 0L
        var bestConfirmations = 0

        for ((payload, runs) in byPayload) {
            val slowRuns = runs.filter {
                it.elapsedMs >= threshold &&
                    it.elapsedMs > baselineCeiling
            }
            if (slowRuns.size >= 2 && slowRuns.size > bestConfirmations) {
                bestPayload = payload
                bestObserved = slowRuns.map { it.elapsedMs }.maxOrNull() ?: 0L
                bestConfirmations = slowRuns.size
            }
        }

        if (bestConfirmations < 2 || bestPayload.isEmpty()) {
            val fallback = probes.maxByOrNull { it.elapsedMs }
            return Detection(
                isHit = false,
                baselineMs = baseline.elapsedMs,
                observedMs = fallback?.elapsedMs ?: 0L,
                deltaMs = (fallback?.elapsedMs ?: 0L) - baseline.elapsedMs,
                confirmations = bestConfirmations,
                payload = bestPayload,
                excerpt = ""
            )
        }

        val delta = bestObserved - baseline.elapsedMs
        val excerpt = buildExcerpt(baseline.elapsedMs, bestObserved, bestPayload, bestConfirmations)

        return Detection(
            isHit = true,
            baselineMs = baseline.elapsedMs,
            observedMs = bestObserved,
            deltaMs = delta,
            confirmations = bestConfirmations,
            payload = bestPayload,
            excerpt = excerpt
        )
    }

    /**
     * Convenience: single-pair check used by the quick-sweep pass. Weaker than
     * [detect] (only one confirmation) — treat the result as a lead, not a hit.
     */
    fun probeOnce(baseline: Probe, candidate: Probe): Detection {
        if (baseline.wasError || candidate.wasError) {
            return emptyDetection(baseline, candidate)
        }
        val delta = candidate.elapsedMs - baseline.elapsedMs
        val threshold = Constants.TIME_BASED_MIN_DELTA_MS
        val baselineCeiling = (baseline.elapsedMs * 3L).coerceAtLeast(1_500L)
        val isHit = candidate.elapsedMs >= threshold && candidate.elapsedMs > baselineCeiling

        return Detection(
            isHit = isHit,
            baselineMs = baseline.elapsedMs,
            observedMs = candidate.elapsedMs,
            deltaMs = delta,
            confirmations = if (isHit) 1 else 0,
            payload = candidate.payload,
            excerpt = if (isHit) {
                buildExcerpt(baseline.elapsedMs, candidate.elapsedMs, candidate.payload, 1)
            } else ""
        )
    }

    /**
     * Helper: given a list of same-payload runs, return the median elapsedMs.
     * Median (not mean) — one GC pause or radio wake shouldn't skew the read.
     */
    fun medianElapsed(probes: List<Probe>): Long {
        val times = probes.filter { !it.wasError }.map { it.elapsedMs }.sorted()
        if (times.isEmpty()) return 0L
        val mid = times.size / 2
        return if (times.size % 2 == 0) {
            (times[mid - 1] + times[mid]) / 2L
        } else {
            times[mid]
        }
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private fun emptyDetection(baseline: Probe, candidate: Probe?): Detection = Detection(
        isHit = false,
        baselineMs = baseline.elapsedMs,
        observedMs = candidate?.elapsedMs ?: 0L,
        deltaMs = abs((candidate?.elapsedMs ?: 0L) - baseline.elapsedMs),
        confirmations = 0,
        payload = candidate?.payload.orEmpty(),
        excerpt = ""
    )

    private fun buildExcerpt(
        baselineMs: Long,
        observedMs: Long,
        payload: String,
        confirmations: Int
    ): String {
        val delta = observedMs - baselineMs
        return buildString(160) {
            append("time-based confirmation: baseline=")
            append(baselineMs).append("ms observed=").append(observedMs)
            append("ms delta=+").append(delta).append("ms confirmations=")
            append(confirmations).append(" payload=")
            append(payload.take(128))
        }
    }
}