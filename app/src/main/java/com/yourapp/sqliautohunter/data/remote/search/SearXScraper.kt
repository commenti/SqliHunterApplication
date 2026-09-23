package com.yourapp.sqliautohunter.data.remote.search

import com.yourapp.sqliautohunter.util.Constants
import com.yourapp.sqliautohunter.util.UserAgentProvider
import okhttp3.OkHttpClient
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.util.concurrent.TimeUnit

class SearXScraper : SearchEngineClient {

    private var customBaseUrl: String? = null

    constructor() : super(createClient(), DEFAULT_BASE_URL)

    constructor(baseUrl: String) : super(createClient(), baseUrl) {
        customBaseUrl = baseUrl
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://searx.space"

        private fun createClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(Constants.NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(Constants.NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(Constants.NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .followRedirects(true)
                .build()
        }
    }

    override suspend fun search(query: String, maxResults: Int): List<String> {
        return scrape(query, maxResults)
    }

    override suspend fun scrape(query: String, maxResults: Int, useProxy: Boolean, proxyUrl: String?): List<String> {
        val results = mutableListOf<String>()
        var currentPage = 1
        var totalResults = 0

        while (totalResults < maxResults && currentPage <= Constants.MAX_SEARCH_PAGES) {
            val pageResults = scrapePage(query, currentPage, maxResults - totalResults)
            results.addAll(pageResults)
            totalResults += pageResults.size
            currentPage++
            
            if (pageResults.isEmpty()) break
        }

        return results.take(maxResults).distinct()
    }

    private suspend fun scrapePage(query: String, page: Int, maxResults: Int): List<String> {
        val headers = UserAgentProvider.getRandomHeaders()
        val url = buildSearchUrl(query, page)
        
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
        return parseSearXResults(html, maxResults)
    }

    private fun buildSearchUrl(query: String, page: Int): String {
        val baseUrl = customBaseUrl ?: DEFAULT_BASE_URL
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        return "$baseUrl/?q=$encodedQuery&p=$page"
    }

    private fun parseSearXResults(html: String, maxResults: Int): List<String> {
        val urls = mutableListOf<String>()
        
        try {
            val doc: Document = Jsoup.parse(html)
            
            // SearX result links are typically in div.result a
            val resultLinks = doc.select("div.result a[href^=http]")
            
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
            
            // Alternative: article.result a
            if (urls.isEmpty()) {
                val alternativeLinks = doc.select("article.result a[href^=http]")
                for (link in alternativeLinks) {
                    val href = link.attr("href")
                    if (href.isNotEmpty()) {
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
                    if (href.isNotEmpty()) {
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

    fun setCustomBaseUrl(url: String) {
        customBaseUrl = if (url.endsWith("/")) url else "$url/"
    }

    fun getCurrentBaseUrl(): String {
        return customBaseUrl ?: DEFAULT_BASE_URL
    }
}
