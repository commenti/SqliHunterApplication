package com.yourapp.sqliautohunter.util

import java.net.URI
import java.net.URISyntaxException

object UrlNormalizer {

    fun normalize(url: String): String {
        return try {
            val uri = URI(url)
            val normalized = normalizeUri(uri)
            normalized.toString()
        } catch (e: URISyntaxException) {
            // Try to fix common URL issues
            val fixedUrl = fixUrl(url)
            try {
                val uri = URI(fixedUrl)
                val normalized = normalizeUri(uri)
                normalized.toString()
            } catch (e2: URISyntaxException) {
                // If still failing, return lowercase version
                url.lowercase()
            }
        }
    }

    private fun normalizeUri(uri: URI): URI {
        val scheme = uri.scheme?.lowercase() ?: "https"
        val host = uri.host?.lowercase() ?: ""
        val path = normalizePath(uri.path)
        val query = normalizeQuery(uri.query)
        val fragment = uri.fragment ?: ""

        return URI(
            scheme,
            uri.userInfo,
            host,
            uri.port,
            path,
            query,
            fragment
        )
    }

    private fun normalizePath(path: String?): String {
        if (path.isNullOrEmpty()) return ""
        
        // Remove trailing slash
        var normalized = path.removeSuffix("/")
        
        // Remove duplicate slashes
        while (normalized.contains("//")) {
            normalized = normalized.replace("//", "/")
        }
        
        return if (normalized.isEmpty()) "/" else normalized
    }

    private fun normalizeQuery(query: String?): String? {
        if (query.isNullOrEmpty()) return null
        
        // Split query parameters
        val params = query.split("&").toMutableList()
        
        // Sort parameters alphabetically by key
        params.sortBy { param ->
            val key = param.substringBefore("=").lowercase()
            key
        }
        
        // Remove empty parameters
        params.removeAll { it.isEmpty() || it == "=" }
        
        return if (params.isEmpty()) null else params.joinToString("&")
    }

    private fun fixUrl(url: String): String {
        var fixed = url
        
        // Add scheme if missing
        if (!fixed.contains("://")) {
            fixed = "https://$fixed"
        }
        
        // Remove spaces
        fixed = fixed.replace(" ", "")
        
        return fixed
    }

    fun extractDomain(url: String): String {
        return try {
            val uri = URI(url)
            uri.host ?: ""
        } catch (e: URISyntaxException) {
            ""
        }
    }

    fun isValidUrl(url: String): Boolean {
        return try {
            val uri = URI(url)
            uri.scheme != null && uri.host != null
        } catch (e: URISyntaxException) {
            false
        }
    }

    fun getBaseUrl(url: String): String {
        return try {
            val uri = URI(url)
            val scheme = uri.scheme ?: "https"
            val host = uri.host ?: ""
            val port = if (uri.port != -1) ":${uri.port}" else ""
            "$scheme://$host$port"
        } catch (e: URISyntaxException) {
            ""
        }
    }
}
