package com.yourapp.sqliautohunter.domain.usecase

import com.yourapp.sqliautohunter.util.UrlNormalizer

class NormalizeUrlUseCase {

    operator fun invoke(url: String): String {
        return UrlNormalizer.normalize(url)
    }

    fun normalizeBatch(urls: List<String>): List<String> {
        return urls.map { url ->
            invoke(url)
        }
    }

    fun normalizeAndFilter(urls: List<String>): List<String> {
        return urls.mapNotNull { url ->
            val normalized = invoke(url)
            if (normalized.isNotEmpty() && UrlNormalizer.isValidUrl(normalized)) {
                normalized
            } else {
                null
            }
        }.distinct()
    }

    fun getNormalizedWithOriginals(urls: List<String>): Map<String, String> {
        return urls.associate { url ->
            val normalized = invoke(url)
            normalized to url
        }
    }

    fun getDomain(url: String): String {
        return UrlNormalizer.extractDomain(url)
    }

    fun getBaseUrl(url: String): String {
        return UrlNormalizer.getBaseUrl(url)
    }

    fun isValidUrl(url: String): Boolean {
        return UrlNormalizer.isValidUrl(url)
    }

    fun filterValidUrls(urls: List<String>): List<String> {
        return urls.filter { url ->
            isValidUrl(url)
        }
    }
}
