package com.yourapp.sqliautohunter.domain.usecase

import com.yourapp.sqliautohunter.util.Constants
import com.yourapp.sqliautohunter.util.UrlNormalizer

class FilterBlacklistedDomainUseCase(
    private val normalizeUrlUseCase: NormalizeUrlUseCase
) {

    private var customBlacklist: List<String> = Constants.DEFAULT_BLACKLIST_DOMAINS

    operator fun invoke(url: String): Boolean {
        val domain = normalizeUrlUseCase.getDomain(url).lowercase()
        return !isBlacklisted(domain)
    }

    fun filterList(urls: List<String>): List<String> {
        return urls.filter { url ->
            invoke(url)
        }
    }

    fun getBlacklisted(urls: List<String>): List<String> {
        return urls.filter { url ->
            !invoke(url)
        }
    }

    fun isBlacklisted(domain: String): Boolean {
        val lowerDomain = domain.lowercase()
        return customBlacklist.any { blacklisted ->
            lowerDomain.contains(blacklisted.lowercase())
        }
    }

    fun updateBlacklist(newBlacklist: List<String>) {
        customBlacklist = newBlacklist
    }

    fun addToBlacklist(domain: String) {
        customBlacklist = customBlacklist + domain.lowercase()
    }

    fun removeFromBlacklist(domain: String) {
        customBlacklist = customBlacklist - domain.lowercase()
    }

    fun getCurrentBlacklist(): List<String> {
        return customBlacklist
    }

    fun resetToDefaults() {
        customBlacklist = Constants.DEFAULT_BLACKLIST_DOMAINS
    }

    fun filterAndNormalize(urls: List<String>): List<String> {
        return urls.mapNotNull { url ->
            val normalized = normalizeUrlUseCase(url)
            if (normalized.isNotEmpty() && invoke(normalized)) {
                normalized
            } else {
                null
            }
        }.distinct()
    }
}
