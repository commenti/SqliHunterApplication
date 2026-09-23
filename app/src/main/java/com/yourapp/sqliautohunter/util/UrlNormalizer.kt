package com.yourapp.sqliautohunter.util

import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.Locale

/**
 * Canonical URL normalization for dedup hashing.
 *
 * Two URLs that resolve to the same resource must produce the same normalized
 * string, or the SHA-256 dedup ledger is theater. Rules applied, in order:
 *
 *   1. Trim whitespace, reject blanks.
 *   2. Lowercase scheme and host only (path + query are case-sensitive).
 *   3. Drop default ports (80 for http, 443 for https).
 *   4. Drop fragment (#...).
 *   5. Strip trailing slash from path unless path is exactly "/".
 *   6. Decode percent-escapes, then re-encode canonically (RFC 3986 unreserved
 *      stays literal, everything else gets uppercase hex).
 *   7. Parse query params, drop tracking params from [TRACKING_PARAMS], sort
 *      remaining by (key, value), re-emit.
 *   8. Collapse repeated slashes in the path.
 *
 * Never throws — returns null on malformed input so callers can route to the
 * error bucket instead of crashing a worker.
 */
object UrlNormalizer {

    private val TRACKING_PARAMS: Set<String> = setOf(
        "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content",
        "utm_id", "utm_name", "utm_reader", "utm_place", "utm_pubreferrer",
        "gclid", "fbclid", "msclkid", "yclid", "dclid", "wbraid", "gbraid",
        "_ga", "_gl", "mc_cid", "mc_eid", "igshid", "ref", "ref_src",
        "ref_url", "referrer", "source", "spm", "scm"
    )

    private const val UNRESERVED_EXTRA = "-._~"

    data class Normalized(
        val normalized: String,
        val scheme: String,
        val host: String,
        val path: String,
        val queryKeys: List<String>
    )

    /** Returns the canonical string, or null if the URL is malformed. */
    fun normalize(rawUrl: String?): String? = parse(rawUrl)?.normalized

    /** Full structured parse — useful for logging and query-key analytics. */
    fun parse(rawUrl: String?): Normalized? {
        if (rawUrl.isNullOrBlank()) return null
        val trimmed = rawUrl.trim()
        if (trimmed.length > Constants.URL_MAX_LEN) return null

        val withScheme = if (trimmed.startsWith("http://", true) ||
            trimmed.startsWith("https://", true)
        ) trimmed else "http://$trimmed"

        val uri = try {
            URI(withScheme)
        } catch (_: Exception) {
            return null
        }

        val scheme = (uri.scheme ?: return null).lowercase(Locale.ROOT)
        if (scheme != "http" && scheme != "https") return null

        val host = (uri.host ?: return null).lowercase(Locale.ROOT).removeSuffix(".")
        if (host.isEmpty()) return null

        val rawPath = uri.rawPath ?: ""
        val path = canonicalPath(rawPath)

        val port = uri.port
        val portPart = when {
            port == -1 -> ""
            scheme == "http" && port == 80 -> ""
            scheme == "https" && port == 443 -> ""
            else -> ":$port"
        }

        val queryKeys = mutableListOf<String>()
        val queryPart = canonicalQuery(uri.rawQuery, queryKeys)

        val normalized = buildString(64) {
            append(scheme).append("://").append(host).append(portPart).append(path)
            if (queryPart.isNotEmpty()) append('?').append(queryPart)
        }

        if (normalized.length > Constants.URL_MAX_LEN) return null
        return Normalized(normalized, scheme, host, path, queryKeys)
    }

    // ------------------------------------------------------------------
    // Path
    // ------------------------------------------------------------------

    private fun canonicalPath(rawPath: String): String {
        if (rawPath.isEmpty()) return "/"

        // Collapse repeated slashes, preserving a leading slash.
        val collapsed = StringBuilder(rawPath.length)
        var lastSlash = false
        for (ch in rawPath) {
            if (ch == '/') {
                if (!lastSlash) collapsed.append('/')
                lastSlash = true
            } else {
                collapsed.append(ch)
                lastSlash = false
            }
        }

        // Decode -> re-encode canonically segment by segment.
        val segments = collapsed.toString().split('/')
        val rebuilt = StringBuilder(rawPath.length)
        segments.forEachIndexed { i, seg ->
            if (i > 0) rebuilt.append('/')
            rebuilt.append(reencode(seg))
        }

        var out = rebuilt.toString()
        if (out.length > 1 && out.endsWith('/')) out = out.dropLast(1)
        return if (out.isEmpty()) "/" else out
    }

    // ------------------------------------------------------------------
    // Query
    // ------------------------------------------------------------------

    private fun canonicalQuery(rawQuery: String?, outKeys: MutableList<String>): String {
        if (rawQuery.isNullOrEmpty()) return ""

        val pairs = rawQuery.split('&').mapNotNull { token ->
            if (token.isEmpty()) return@mapNotNull null
            val eq = token.indexOf('=')
            val rawKey = if (eq < 0) token else token.substring(0, eq)
            val rawVal = if (eq < 0) "" else token.substring(eq + 1)
            val key = safeDecode(rawKey) ?: return@mapNotNull null
            val value = safeDecode(rawVal) ?: ""
            if (key.lowercase(Locale.ROOT) in TRACKING_PARAMS) return@mapNotNull null