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

class WebViewPoolManager(private val appContext: Context) {

    private val webViewPool = mutableListOf<WebViewWrapper>()
    private val mutex = Mutex()
    private var maxPoolSize = Constants.DEFAULT_CONCURRENCY
    private var currentPoolSize = 0

    data class WebViewWrapper(
        val webView: WebView,
        var isInUse: Boolean = false,
        var lastUsedTime: Long = 0,
        var url: String? = null
    )

    // Pool starts empty on purpose: WebViews are heavyweight, must be created
    // on the main thread, and do slow disk init. Creating them eagerly in init
    // stalls first-frame composition (startup ANR) and crashes when Hilt
    // happens to build this singleton off the main thread (Android 12+).
    // Instances are created on demand in acquireWebView() instead.

    suspend fun setMaxPoolSize(size: Int) {
        maxPoolSize = size
        // If current pool is smaller than max, extra instances are created
        // lazily on the main thread by the next acquireWebView() call.
        // If current pool is larger than max, remove excess
        while (currentPoolSize > maxPoolSize) {
            removeWebViewFromPool()
        }
    }

    private suspend fun addWebViewToPool(): WebViewWrapper {
        // WebView MUST be constructed on the main thread; callers of
        // acquireWebView() may be on Dispatchers.IO.
        val webView = withContext(Dispatchers.Main) {
            createWebView()
        }
        val wrapper = WebViewWrapper(webView = webView)
        webViewPool.add(wrapper)
        currentPoolSize++
        return wrapper
    }

    private suspend fun removeWebViewFromPool(): WebViewWrapper? {
        val unusedWebViews = webViewPool.filter { !it.isInUse }
        if (unusedWebViews.isNotEmpty()) {
            val wrapper = unusedWebViews.first()
            webViewPool.remove(wrapper)
            currentPoolSize--
            // WebView.destroy() must run on the main thread.
            withContext(Dispatchers.Main) {
                try {
                    wrapper.webView.destroy()
                } catch (e: Exception) {
                    // Ignore errors during cleanup
                }
            }
            return wrapper
        }
        return null
    }

    private fun createWebView(): WebView {
        return WebView(appContext).apply {
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

    private suspend fun clearWebView(webView: WebView) {
        // All WebView methods must run on the main thread.
        withContext(Dispatchers.Main) {
            try {
                webView.clearCache(true)
                webView.clearHistory()
                webView.clearFormData()
                webView.loadUrl("about:blank")
            } catch (e: Exception) {
                // Ignore errors during cleanup
            }
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

    suspend fun destroy() {
        val wrappers = mutex.withLock {
            val copy = webViewPool.toList()
            webViewPool.clear()
            currentPoolSize = 0
            copy
        }
        withContext(Dispatchers.Main) {
            wrappers.forEach { wrapper ->
                try {
                    wrapper.webView.destroy()
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
    }

    // Clean up unused WebViews after a period of inactivity
    suspend fun cleanupInactiveWebViews(inactiveThresholdMs: Long = 300000) {
        val toDestroy = mutex.withLock {
            val now = System.currentTimeMillis()
            val inactiveWebViews = webViewPool.filter { wrapper ->
                !wrapper.isInUse && (now - wrapper.lastUsedTime) > inactiveThresholdMs
            }

            inactiveWebViews.forEach { wrapper ->
                webViewPool.remove(wrapper)
                currentPoolSize--
            }
            inactiveWebViews
        }
        withContext(Dispatchers.Main) {
            toDestroy.forEach { wrapper ->
                try {
                    wrapper.webView.destroy()
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
    }

    // Get a WebView that's ready for a specific URL
    suspend fun getWebViewForUrl(url: String): WebViewWrapper? {
        return acquireWebView(url)
    }
}
