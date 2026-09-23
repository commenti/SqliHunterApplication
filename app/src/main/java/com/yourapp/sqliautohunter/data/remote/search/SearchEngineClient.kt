package com.yourapp.sqliautohunter.data.remote.search

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

abstract class SearchEngineClient(
    protected val client: OkHttpClient,
    protected val baseUrl: String
) {

    protected val timeoutSeconds = 30L

    init {
        val builder = OkHttpClient.Builder()
            .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .followRedirects(true)
        
        // This allows subclasses to customize the client
    }

    protected open fun buildRequest(url: String, headers: Map<String, String> = emptyMap()): Request {
        val requestBuilder = Request.Builder()
            .url(url)
            .get()
        
        headers.forEach { (key, value) ->
            requestBuilder.addHeader(key, value)
        }
        
        // Add default headers
        requestBuilder.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
        requestBuilder.addHeader("Accept-Language", "en-US,en;q=0.5")
        requestBuilder.addHeader("Connection", "keep-alive")
        
        return requestBuilder.build()
    }

    protected suspend fun executeRequest(request: Request): Response {
        return try {
            client.newCall(request).execute()
        } catch (e: IOException) {
            throw SearchEngineException("Network error: ${e.message}")
        } catch (e: Exception) {
            throw SearchEngineException("Request failed: ${e.message}")
        }
    }

    protected fun getResponseBody(response: Response): String {
        return try {
            response.body?.string() ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    abstract suspend fun search(query: String, maxResults: Int = 10): List<String>

    abstract suspend fun scrape(query: String, maxResults: Int, useProxy: Boolean = false, proxyUrl: String? = null): List<String>

    protected fun extractUrlsFromHtml(html: String): List<String> {
        val urlRegex = "(https?://[^\\s<>\\\"]+)".toRegex()
        val matches = urlRegex.findAll(html)
        return matches.map { it.value }.distinct().toList()
    }

    protected fun cleanUrl(url: String): String {
        return url
            .replace("about:", "")
            .replace("javascript:", "")
            .replace("mailto:", "")
            .trim()
    }

    class SearchEngineException(message: String) : Exception(message)
}
