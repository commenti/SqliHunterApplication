package com.yourapp.sqliautohunter.engine.browser

import android.webkit.WebView
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.yourapp.sqliautohunter.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class CdpSessionController(private val webViewPoolManager: WebViewPoolManager) {

    private var currentSession: WebViewWrapper? = null

    private data class WebViewWrapper(
        val webView: WebView,
        var isInUse: Boolean = false
    )

    suspend fun createSession(url: String): Boolean {
        return withContext(Dispatchers.IO) {
            val wrapper = webViewPoolManager.acquireWebView(url) ?: return@withContext false
            
            try {
                // Enable DevTools Protocol if available
                if (WebViewFeature.isFeatureSupported(WebViewFeature.DEVTOOLS)) {
                    WebViewCompat.enableDevTools(wrapper.webView, true)
                }
                
                // Navigate to the URL
                wrapper.webView.loadUrl(url)
                
                // Wait for page to load
                val loaded = withTimeoutOrNull(Constants.WEBVIEW_TIMEOUT_MS) {
                    waitForPageLoad(wrapper.webView)
                } != null
                
                if (!loaded) {
                    webViewPoolManager.releaseWebView(wrapper)
                    return@withContext false
                }
                
                currentSession = WebViewWrapper(wrapper.webView, true)
                true
            } catch (e: Exception) {
                webViewPoolManager.releaseWebView(wrapper)
                false
            }
        }
    }

    suspend fun navigate(url: String): Boolean {
        return withContext(Dispatchers.IO) {
            val wrapper = webViewPoolManager.acquireWebView(url) ?: return@withContext false
            
            try {
                wrapper.webView.loadUrl(url)
                
                val loaded = withTimeoutOrNull(Constants.WEBVIEW_TIMEOUT_MS) {
                    waitForPageLoad(wrapper.webView)
                } != null
                
                if (!loaded) {
                    webViewPoolManager.releaseWebView(wrapper)
                    return@withContext false
                }
                
                currentSession = WebViewWrapper(wrapper.webView, true)
                true
            } catch (e: Exception) {
                webViewPoolManager.releaseWebView(wrapper)
                false
            }
        }
    }

    suspend fun executeJavaScript(script: String): String? {
        return withContext(Dispatchers.IO) {
            val webView = currentSession?.webView ?: return@withContext null
            
            try {
                val result = webView.evaluateJavascript(script) { result ->
                    result?.toString()
                }
                result
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun getPageContent(): String? {
        return withContext(Dispatchers.IO) {
            val webView = currentSession?.webView ?: return@withContext null
            
            try {
                val result = webView.evaluateJavascript("document.documentElement.outerHTML") { result ->
                    result?.toString()
                }
                result
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun getPageTitle(): String? {
        return withContext(Dispatchers.IO) {
            val webView = currentSession?.webView ?: return@withContext null
            
            try {
                val result = webView.evaluateJavascript("document.title") { result ->
                    result?.toString()
                }
                result
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun getCurrentUrl(): String? {
        return withContext(Dispatchers.IO) {
            currentSession?.webView?.url
        }
    }

    suspend fun injectPayload(payload: String): Boolean {
        return withContext(Dispatchers.IO) {
            val webView = currentSession?.webView ?: return@withContext false
            
            try {
                // Inject payload into URL or form
                val currentUrl = webView.url ?: return@withContext false
                val testUrl = injectPayloadIntoUrl(currentUrl, payload)
                
                webView.loadUrl(testUrl)
                
                withTimeoutOrNull(Constants.WEBVIEW_TIMEOUT_MS) {
                    waitForPageLoad(webView)
                } != null
            } catch (e: Exception) {
                false
            }
        }
    }

    private fun injectPayloadIntoUrl(url: String, payload: String): String {
        return when {
            url.contains("?") -> {
                if (url.endsWith("?") || url.endsWith("&")) {
                    "$url$payload"
                } else {
                    "$url&$payload"
                }
            }
            url.contains("=") -> {
                val separator = if (url.endsWith("=")) "" else "="
                "$url$separator$payload"
            }
            else -> {
                "$url?$payload"
            }
        }
    }

    private suspend fun waitForPageLoad(webView: WebView): Boolean {
        return withContext(Dispatchers.IO) {
            // Simple wait - in real implementation, use WebViewClient callbacks
            delay(2000) // Wait for page to load
            true
        }
    }

    suspend fun captureResponse(url: String, payload: String): ResponseData? {
        return withContext(Dispatchers.IO) {
            val wrapper = webViewPoolManager.acquireWebView(url) ?: return@withContext null
            
            try {
                val testUrl = injectPayloadIntoUrl(url, payload)
                wrapper.webView.loadUrl(testUrl)
                
                val loaded = withTimeoutOrNull(Constants.WEBVIEW_TIMEOUT_MS) {
                    waitForPageLoad(wrapper.webView)
                } != null
                
                if (!loaded) {
                    webViewPoolManager.releaseWebView(wrapper)
                    return@withContext null
                }
                
                val content = wrapper.webView.evaluateJavascript("document.documentElement.outerHTML") { result ->
                    result?.toString()
                }
                
                val responseTime = System.currentTimeMillis() - wrapper.lastUsedTime
                
                ResponseData(
                    url = testUrl,
                    content = content ?: "",
                    responseTimeMs = responseTime,
                    statusCode = 200 // Assume success for WebView
                )
            } catch (e: Exception) {
                webViewPoolManager.releaseWebView(wrapper)
                null
            } finally {
                webViewPoolManager.releaseWebView(wrapper)
            }
        }
    }

    suspend fun closeSession() {
        withContext(Dispatchers.IO) {
            currentSession?.let { wrapper ->
                webViewPoolManager.releaseWebView(
                    com.yourapp.sqliautohunter.engine.browser.WebViewPoolManager.WebViewWrapper(
                        wrapper.webView,
                        false
                    )
                )
                currentSession = null
            }
        }
    }

    data class ResponseData(
        val url: String,
        val content: String,
        val responseTimeMs: Long,
        val statusCode: Int
    )
}
