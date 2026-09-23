package com.yourapp.sqliautohunter.util

import java.security.MessageDigest

object HashUtils {

    fun sha256(input: String): String {
        val bytes = input.toByteArray(Charsets.UTF_8)
        val md = MessageDigest.getInstance(Constants.HASH_ALGORITHM)
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    fun sha256(bytes: ByteArray): String {
        val md = MessageDigest.getInstance(Constants.HASH_ALGORITHM)
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    fun md5(input: String): String {
        val bytes = input.toByteArray(Charsets.UTF_8)
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    fun generateContentHash(content: String): String {
        return sha256(content)
    }

    fun verifyHash(input: String, expectedHash: String): Boolean {
        return sha256(input) == expectedHash
    }
}
