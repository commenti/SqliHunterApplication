package com.yourapp.sqliautohunter.data.remote.search

import com.yourapp.sqliautohunter.util.Constants
import com.yourapp.sqliautohunter.util.UserAgentProvider
import okhttp3.OkHttpClient
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.util.concurrent.TimeUnit

class BingScraper : SearchEngineClient {

    private val searchEndpoint = "$BASE_URL/search"

    constructor() : super(createClient(), "https://www.bing.com")

    companion object {
        const val BASE_URL = "https://www.bing.com"

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
        var currentPage = 0
        var totalResults = 0

        while (totalResults < maxResults && currentPage < Constants.MAX_SEARCH_PAGES) {
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
                // Rate limited, wait and retry
                Thread.sleep(Constants.RETRY_BACKOFF_BASE_MS)
                return emptyList()
            }
            return emptyList()
        }

        val html = getResponseBody(response)
        return parseBingResults(html, maxResults)
    }

    private fun buildSearchUrl(query: String, page: Int): String {
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        val startParam = if (page > 0) "&first=${page * 10 + 1}" else ""
        return "$searchEndpoint?q=$encodedQuery$startParam"
    }

    private fun parseBingResults(html: String, maxResults: Int): List<String> {
        val urls = mutableListOf<String>()
        
        try {
            val doc: Document = Jsoup.parse(html)
            
            // Bing result links are in li.b_algo elements with h2 a tags
            val resultLinks = doc.select("li.b_algo a")
            
            for (link in resultLinks) {
                val href = link.attr("href")
                if (href.isNotEmpty() && !href.startsWith("http://www.bing.com") && !href.startsWith("https://www.bing.com")) {
                    val cleanUrl = cleanUrl(href)
                    if (isValidUrl(cleanUrl)) {
                        urls.add(cleanUrl)
                    }
                }
                
                if (urls.size >= maxResults) break
            }
            
            // Also check for ol#b_results li.b_algo
            if (urls.isEmpty()) {
                val alternativeLinks = doc.select("ol#b_results li.b_algo a")
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
            
            // Try generic a tags with href starting with http
            if (urls.isEmpty()) {
                val genericLinks = doc.select("a[href^=http]")
                for (link in genericLinks) {
                    val href = link.attr("href")
                    if (href.isNotEmpty() && !href.contains("bing.com")) {
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

    private fun cleanBingUrl(url: String): String {
        return url
            .replace("\u0026", "&")
            .replace("\u003f", "?")
            .replace("\u0025", "%")
    }
}
