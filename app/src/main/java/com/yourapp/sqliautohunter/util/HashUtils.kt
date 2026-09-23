package com.yourapp.sqliautohunter.util

import java.security.MessageDigest

/**
 * Hashing helpers. SHA-256 hex is the canonical dedup key for normalized URLs.
 *
 * All functions are thread-safe — MessageDigest instances are created per call
 * (not pooled) because the cost of allocation is dwarfed by the URL normalization
 * work that precedes it, and pooling invites cross-thread reuse bugs.
 */
object HashUtils {

    private val HEX = "0123456789abcdef".toCharArray()

    /** SHA-256 of UTF-8 bytes, lowercase hex, 64 chars. */
    fun sha256Hex(input: String): String = sha256Hex(input.toByteArray(Charsets.UTF_8))

    /** SHA-256 of raw bytes, lowercase hex, 64 chars. */
    fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance(Constants.HASH_ALGO_SHA256).digest(bytes)
        return toHex(digest)
    }

    /** SHA-256 of a normalized URL. Null-safe: empty string hashes like any other. */
    fun hashUrl(normalizedUrl: String): String = sha256Hex(normalizedUrl)

    /**
     * SHA-256 of a payload. Used for grouping identical payloads across
     * keyword sources without storing the full string as an index key.
     */
    fun hashPayload(payload: String): String = sha256Hex(payload)

    /** Fast non-cryptographic FNV-1a 64-bit — for in-memory buckets only. */
    fun fnv1a64(input: String): Long {
        var hash = 0xcbf29ce484222325uL
        for (b in input.toByteArray(Charsets.UTF_8)) {
            hash = hash xor (b.toUByte().toULong())
            hash *= 0x100000001b3uL
        }
        return hash.toLong()
    }

    private fun toHex(bytes: ByteArray): String {
        val out = CharArray(bytes.size * 2)
        var i = 0
        for (b in bytes) {
            val v = b.toInt() and 0xFF
            out[i++] = HEX[v ushr 4]
            out[i++] = HEX[v and 0x0F]
        }
        return String(out)
    }
}