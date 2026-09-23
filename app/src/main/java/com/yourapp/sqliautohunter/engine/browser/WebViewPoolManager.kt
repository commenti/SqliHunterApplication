package com.yourapp.sqliautohunter.engine.browser

import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import com.yourapp.sqliautohunter.util.Constants
import com.yourapp.sqliautohunter.util.UserAgentProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class WebViewPoolManager(private val context: Context) {

    private val webViewPool = mutableListOf<WebViewWrapper>()
    private val mutex = Mutex()
    private var maxPoolSize = Constants.DEFAULT_CONCURRENCY
    private var currentPoolSize = 0

    private data class WebViewWrapper(
        val webView: WebView,
        var isInUse: Boolean = false,
        var lastUsedTime: Long = 0,
        var url: String? = null
    )

    init {
        // Initialize pool with default size
        initializePool(maxPoolSize)
    }

    fun setMaxPoolSize(size: Int) {
        maxPoolSize = size
        // If current pool is smaller than max, add more
        while (currentPoolSize < maxPoolSize) {
            addWebViewToPool()
        }
        // If current pool is larger than max, remove excess
        while (currentPoolSize > maxPoolSize) {
            removeWebViewFromPool()
        }
    }

    private fun initializePool(size: Int) {
        repeat(size) {
            addWebViewToPool()
        }
    }

    private fun addWebViewToPool(): WebViewWrapper {
        val webView = createWebView()
        val wrapper = WebViewWrapper(webView = webView)
        webViewPool.add(wrapper)
        currentPoolSize++
        return wrapper
    }

    private fun removeWebViewFromPool(): WebViewWrapper? {
        val unusedWebViews = webViewPool.filter { !it.isInUse }
        if (unusedWebViews.isNotEmpty()) {
            val wrapper = unusedWebViews.first()
            webViewPool.remove(wrapper)
            currentPoolSize--
            // Clean up the WebView
            wrapper.webView.destroy()
            return wrapper
        }
        return null
    }

    private fun createWebView(): WebView {
        return WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.setSupportZoom(false)
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.javaScriptCanOpenWindowsAutomatically = true
            settings.userAgentString = UserAgentProvider.getRandomUserAgent()
            
            // Set WebViewClient to prevent external browser
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    return false // Let WebView handle the URL
                }
            }
        }
    }

    suspend fun acquireWebView(url: String? = null): WebViewWrapper? {
        return mutex.withLock {
            // First, try to find an available WebView
            val availableWebView = webViewPool.firstOrNull { !it.isInUse }
            
            if (availableWebView != null) {
                availableWebView.isInUse = true
                availableWebView.lastUsedTime = System.currentTimeMillis()
                availableWebView.url = url
                return availableWebView
            }

            // If no available WebView and pool is not at max size, create a new one
            if (currentPoolSize < maxPoolSize) {
                val newWebView = addWebViewToPool()
                newWebView.isInUse = true
                newWebView.lastUsedTime = System.currentTimeMillis()
                newWebView.url = url
                return newWebView
            }

            // Pool is full and all WebViews are in use
            null
        }
    }

    suspend fun releaseWebView(wrapper: WebViewWrapper) {
        mutex.withLock {
            wrapper.isInUse = false
            wrapper.url = null
            // Clear the WebView state
            clearWebView(wrapper.webView)
        }
    }

    private fun clearWebView(webView: WebView) {
        try {
            webView.clearCache(true)
            webView.clearHistory()
            webView.clearFormData()
            webView.loadUrl("about:blank")
        } catch (e: Exception) {
            // Ignore errors during cleanup
        }
    }

    suspend fun releaseAll() {
        mutex.withLock {
            webViewPool.forEach { wrapper ->
                wrapper.isInUse = false
                clearWebView(wrapper.webView)
            }
        }
    }

    fun getAvailableCount(): Int {
        return webViewPool.count { !it.isInUse }
    }

    fun getInUseCount(): Int {
        return webViewPool.count { it.isInUse }
    }

    fun getTotalCount(): Int {
        return currentPoolSize
    }

    fun destroy() {
        webViewPool.forEach { wrapper ->
            try {
                wrapper.webView.destroy()
            } catch (e: Exception) {
                // Ignore
            }
        }
        webViewPool.clear()
        currentPoolSize = 0
    }

    // Clean up unused WebViews after a period of inactivity
    suspend fun cleanupInactiveWebViews(inactiveThresholdMs: Long = 300000) {
        mutex.withLock {
            val now = System.currentTimeMillis()
            val inactiveWebViews = webViewPool.filter { wrapper ->
                !wrapper.isInUse && (now - wrapper.lastUsedTime) > inactiveThresholdMs
            }
            
            inactiveWebViews.forEach { wrapper ->
                webViewPool.remove(wrapper)
                try {
                    wrapper.webView.destroy()
                } catch (e: Exception) {
                    // Ignore
                }
                currentPoolSize--
            }
        }
    }

    // Get a WebView that's ready for a specific URL
    suspend fun getWebViewForUrl(url: String): WebViewWrapper? {
        return acquireWebView(url)
    }
}
