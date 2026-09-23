package com.yourapp.sqliautohunter.data.remote.search

import com.yourapp.sqliautohunter.util.Constants
import com.yourapp.sqliautohunter.util.UserAgentProvider
import okhttp3.OkHttpClient
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.util.concurrent.TimeUnit

class DuckDuckGoScraper : SearchEngineClient {

    private val ddgBaseUrl = "https://html.duckduckgo.com"
    private val apiBaseUrl = "https://api.duckduckgo.com"

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(Constants.NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(Constants.NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(Constants.NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    constructor() : super(client, ddgBaseUrl)

    override suspend fun search(query: String, maxResults: Int): List<String> {
        return scrape(query, maxResults)
    }

    override suspend fun scrape(query: String, maxResults: Int, useProxy: Boolean, proxyUrl: String?): List<String> {
        val results = mutableListOf<String>()
        var currentOffset = 0
        var totalResults = 0

        while (totalResults < maxResults && currentOffset < maxResults) {
            val pageResults = scrapePage(query, currentOffset, maxResults - totalResults)
            results.addAll(pageResults)
            totalResults += pageResults.size
            currentOffset += Constants.SEARCH_RESULTS_PER_PAGE
            
            if (pageResults.isEmpty()) break
        }

        return results.take(maxResults).distinct()
    }

    private suspend fun scrapePage(query: String, offset: Int, maxResults: Int): List<String> {
        val headers = UserAgentProvider.getRandomHeaders()
        val url = buildSearchUrl(query, offset)
        
        val request = buildRequest(url, headers)
        val response = executeRequest(request)
        
        if (!response.isSuccessful) {
            if (response.code == 429 || response.code == 403) {
                Thread.sleep(Constants.RETRY_BACKOFF_BASE_MS)
                return emptyList()
            }
            return emptyList()
        }

        val html = getResponseBody(response)
        return parseDuckDuckGoResults(html, maxResults)
    }

    private fun buildSearchUrl(query: String, offset: Int): String {
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        return "$ddgBaseUrl/html/?q=$encodedQuery&kl=us-en&df=&p=$offset"
    }

    private fun parseDuckDuckGoResults(html: String, maxResults: Int): List<String> {
        val urls = mutableListOf<String>()
        
        try {
            val doc: Document = Jsoup.parse(html)
            
            // DuckDuckGo result links are in div.result__body a.result__url
            val resultLinks = doc.select("div.result__body a.result__url")
            
            for (link in resultLinks) {
                val href = link.attr("href")
                if (href.isNotEmpty()) {
                    val cleanUrl = cleanUrl(href)
                    if (isValidUrl(cleanUrl)) {
                        urls.add(cleanUrl)
                    }
                }
                if (urls.size >= maxResults) break
            }
            
            // Alternative: div.result a
            if (urls.isEmpty()) {
                val alternativeLinks = doc.select("div.result a[href^=http]")
                for (link in alternativeLinks) {
                    val href = link.attr("href")
                    if (href.isNotEmpty() && !href.contains("duckduckgo.com")) {
                        val cleanUrl = cleanUrl(href)
                        if (isValidUrl(cleanUrl)) {
                            urls.add(cleanUrl)
                        }
                    }
                    if (urls.size >= maxResults) break
                }
            }
            
            // Fallback to generic a tags
            if (urls.isEmpty()) {
                val genericLinks = doc.select("a[href^=http]")
                for (link in genericLinks) {
                    val href = link.attr("href")
                    if (href.isNotEmpty() && !href.contains("duckduckgo.com")) {
                        val cleanUrl = cleanUrl(href)
                        if (isValidUrl(cleanUrl)) {
                            urls.add(cleanUrl)
                        }
                    }
                    if (urls.size >= maxResults) break
                }
            }
            
        } catch (e: Exception) {
            // Fallback to regex extraction
            urls.addAll(extractUrlsFromHtml(html).filter { isValidUrl(it) })
        }
        
        return urls.take(maxResults).distinct()
    }

    private fun isValidUrl(url: String): Boolean {
        return url.startsWith("http://") || url.startsWith("https://")
    }
}
