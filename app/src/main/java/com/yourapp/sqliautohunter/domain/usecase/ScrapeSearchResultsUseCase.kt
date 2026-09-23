package com.yourapp.sqliautohunter.domain.usecase

import com.yourapp.sqliautohunter.data.remote.search.BingScraper
import com.yourapp.sqliautohunter.data.remote.search.DuckDuckGoScraper
import com.yourapp.sqliautohunter.data.remote.search.SearchEngineClient
import com.yourapp.sqliautohunter.data.remote.search.SearXScraper
import com.yourapp.sqliautohunter.util.UserAgentProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

class ScrapeSearchResultsUseCase(
    private val bingScraper: BingScraper,
    private val duckDuckGoScraper: DuckDuckGoScraper,
    private val searXScraper: SearXScraper
) {

    private val searchEngines = listOf("bing", "duckduckgo", "searx")

    suspend operator fun invoke(
        query: String,
        maxResults: Int = 50,
        useProxy: Boolean = false,
        proxyUrl: String? = null
    ): List<String> {
        return withContext(Dispatchers.IO) {
            val allResults = mutableListOf<String>()
            var currentResults = 0

            // Try each search engine in order until we have enough results
            for (engine in searchEngines) {
                if (currentResults >= maxResults) break

                val results = try {
                    when (engine) {
                        "bing" -> scrapeBing(query, maxResults - currentResults, useProxy, proxyUrl)
                        "duckduckgo" -> scrapeDuckDuckGo(query, maxResults - currentResults, useProxy, proxyUrl)
                        "searx" -> scrapeSearX(query, maxResults - currentResults, useProxy, proxyUrl)
                        else -> emptyList()
                    }
                } catch (e: Exception) {
                    // Log error and continue with next engine
                    continue
                }

                allResults.addAll(results)
                currentResults += results.size
            }

            allResults.distinct()
        }
    }

    private suspend fun scrapeBing(
        query: String,
        maxResults: Int,
        useProxy: Boolean,
        proxyUrl: String?
    ): List<String> {
        return bingScraper.scrape(query, maxResults, useProxy, proxyUrl)
    }

    private suspend fun scrapeDuckDuckGo(
        query: String,
        maxResults: Int,
        useProxy: Boolean,
        proxyUrl: String?
    ): List<String> {
        return duckDuckGoScraper.scrape(query, maxResults, useProxy, proxyUrl)
    }

    private suspend fun scrapeSearX(
        query: String,
        maxResults: Int,
        useProxy: Boolean,
        proxyUrl: String?
    ): List<String> {
        return searXScraper.scrape(query, maxResults, useProxy, proxyUrl)
    }

    suspend fun scrapeWithFallback(
        query: String,
        maxResults: Int = 50,
        useProxy: Boolean = false,
        proxyUrl: String? = null
    ): List<String> {
        return try {
            invoke(query, maxResults, useProxy, proxyUrl)
        } catch (e: Exception) {
            // Fallback to direct scraping with different user agents
            val userAgents = UserAgentProvider.getRandomHeaders()
            try {
                bingScraper.scrape(query, maxResults, useProxy, proxyUrl)
            } catch (e2: Exception) {
                emptyList()
            }
        }
    }

    suspend fun scrapeMultipleQueries(
        queries: List<String>,
        maxResultsPerQuery: Int = 20
    ): List<String> {
        return withContext(Dispatchers.IO) {
            val allResults = mutableListOf<String>()
            
            queries.map { query ->
                async {
                    invoke(query, maxResultsPerQuery)
                }
            }.forEach { deferred ->
                try {
                    allResults.addAll(deferred.await())
                } catch (e: Exception) {
                    // Continue with other queries
                }
            }
            
            allResults.distinct()
        }
    }
}
