package com.yourapp.sqliautohunter.domain.payload

import com.yourapp.sqliautohunter.domain.model.VulnerabilityType
import kotlin.math.abs

/**
 * Boolean-based (blind) SQLi detector.
 *
 * Strategy: send a TRUE/ FALSE payload pair against the same parameter and
 * compare how each response diverges from a control baseline (no payload, or
 * the parameter at its original value). A parameter is boolean-injectable when
 * the TRUE response stays close to the baseline AND the FALSE response diverges
 * meaningfully — the injection flips a condition the application cares about.
 *
 * Similarity metric: a cheap token-overlap ratio. Robust enough for HTML
 * responses where whitespace and nonce changes break byte-equality, cheap
 * enough to run inline on a worker thread. The threshold lives in
 * Constants.BOOLEAN_SIMILARITY_THRESHOLD.
 *
 * Stateless and thread-safe.
 */
class BooleanBasedDetector {

    /**
     * Outcome of a boolean pair probe.
     *
     * `similarityTrue` / `similarityFalse` are token-overlap ratios against the
     * baseline. `delta` is |true - false|. A confident hit wants true near 1.0,
     * false meaningfully below it, and a non-trivial delta.
     */
    data class Detection(
        val isHit: Boolean,
        val similarityTrue: Float,
        val similarityFalse: Float,
        val delta: Float,
        val excerpt: String
    )

    val technique: VulnerabilityType = VulnerabilityType.BOOLEAN_BASED

    fun payloads(cap: Int = 4): List<PayloadTemplate> =
        SqliPayloadTemplates.forTechnique(VulnerabilityType.BOOLEAN_BASED, cap)

    /**
     * Compare a TRUE/FALSE response pair against a baseline.
     *
     * @param baseline    Response body with the parameter untouched (or absent).
     * @param trueBody    Response body after sending the TRUE payload.
     * @param falseBody   Response body after sending the FALSE payload.
     */
    fun detect(
        baseline: String?,
        trueBody: String?,
        falseBody: String?
    ): Detection {
        if (baseline.isNullOrEmpty() || trueBody.isNullOrEmpty() || falseBody.isNullOrEmpty()) {
            return Detection(false, 0f, 0f, 0f, "")
        }

        val simTrue = similarity(baseline, trueBody)
        val simFalse = similarity(baseline, falseBody)
        val delta = abs(simTrue - simFalse)

        val threshold = Constants.BOOLEAN_SIMILARITY_THRESHOLD
        val isHit = simTrue >= threshold && simFalse < threshold && delta >= (1f - threshold)

        val excerpt = if (isHit) {
            excerptAroundDivergence(baseline, falseBody)
        } else {
            ""
        }

        return Detection(
            isHit = isHit,
            similarityTrue = simTrue,
            similarityFalse = simFalse,
            delta = delta,
            excerpt = excerpt
        )
    }

    // ------------------------------------------------------------------
    // Similarity
    // ------------------------------------------------------------------

    /**
     * Token-overlap similarity in [0, 1]. 1.0 means identical token multisets.
     * Whitespace is collapsed, common noise tokens (script/style bodies) are
     * discarded, and the comparison is done on lowercased tokens.
     */
    fun similarity(a: String, b: String): Float {
        if (a == b) return 1f
        if (a.isEmpty() || b.isEmpty()) return 0f

        val ta = tokenize(a)
        val tb = tokenize(b)
        if (ta.isEmpty() || tb.isEmpty()) return 0f

        val counts = HashMap<String, Int>(ta.size)
        for (t in ta) counts[t] = (counts[t] ?: 0) + 1

        var matched = 0
        for (t in tb) {
            val c = counts[t] ?: continue
            if (c > 0) {
                matched++
                counts[t] = c - 1
            }
        }
        val denom = maxOf(ta.size, tb.size)
        return matched.toFloat() / denom.toFloat()
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private fun tokenize(body: String): List<String> {
        val out = ArrayList<String>(estimateTokens(body.length))
        val sb = StringBuilder(32)
        var i = 0
        val n = body.length
        while (i < n) {
            val c = body[i]
            when {
                c.isLetterOrDigit() -> sb.append(c.lowercaseChar())
                c == '-' || c == '_' || c == ':' || c == '/' -> sb.append(c)
                else -> {
                    if (sb.length >= 2) out += sb.toString()
                    sb.setLength(0)
                }
            }
            i++
        }
        if (sb.length >= 2) out += sb.toString()
        return out
    }

    private fun estimateTokens(len: Int): Int = (len / 8).coerceIn(64, 8192)

    private fun excerptAroundDivergence(baseline: String, falseBody: String): String {
        // Find the first offset where the two responses differ meaningfully.
        val maxLen = minOf(baseline.length, falseBody.length)
        var i = 0
        while (i < maxLen && baseline[i] == falseBody[i]) i++
        val start = (i - 64).coerceAtLeast(0)
        val end = (i + Constants.RESPONSE_SNIPPET_MAX_LEN - 64).coerceAtMost(falseBody.length)
        return falseBody.substring(start, end)
    }
}