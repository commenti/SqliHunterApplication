package com.yourapp.sqliautohunter.engine.browser

import android.webkit.WebView
import com.yourapp.sqliautohunter.engine.browser.WebViewPoolManager.WebViewWrapper
import com.yourapp.sqliautohunter.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class CdpSessionController(private val webViewPoolManager: WebViewPoolManager) {

    private var currentSession: WebViewWrapper? = null

    suspend fun createSession(url: String): Boolean {
        return withContext(Dispatchers.Main) {
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
                currentSession = wrapper
                true
            } catch (e: Exception) {
                webViewPoolManager.releaseWebView(wrapper)
                false
            }
        }
    }

    suspend fun navigate(url: String): Boolean {
        return withContext(Dispatchers.Main) {
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
                currentSession = wrapper
                true
            } catch (e: Exception) {
                webViewPoolManager.releaseWebView(wrapper)
                false
            }
        }
    }

    suspend fun executeJavaScript(script: String): String? {
        val webView = currentSession?.webView ?: return null
        return evaluateJs(webView, script)
    }

    suspend fun getPageContent(): String? {
        val webView = currentSession?.webView ?: return null
        return evaluateJs(webView, "document.documentElement.outerHTML")
    }

    suspend fun getPageTitle(): String? {
        val webView = currentSession?.webView ?: return null
        return evaluateJs(webView, "document.title")
    }

    suspend fun getCurrentUrl(): String? {
        return currentSession?.webView?.url
    }

    suspend fun injectPayload(payload: String): Boolean {
        return withContext(Dispatchers.Main) {
            val webView = currentSession?.webView ?: return@withContext false
            try {
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
                if (url.endsWith("?") || url.endsWith("&")) "$url$payload" else "$url&$payload"
            }
            url.contains("=") -> {
                val separator = if (url.endsWith("=")) "" else "="
                "$url$separator$payload"
            }
            else -> "$url?$payload"
        }
    }

    private suspend fun waitForPageLoad(webView: WebView): Boolean {
        delay(2000)
        return true
    }

    private suspend fun evaluateJs(webView: WebView, script: String): String? {
        return withContext(Dispatchers.Main) {
            try {
                suspendCoroutine { cont ->
                    try {
                        webView.evaluateJavascript(script) { result -> cont.resume(result) }
                    } catch (e: Exception) {
                        cont.resume(null as String?)
                    }
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun captureResponse(url: String, payload: String): ResponseData? {
        val wrapper = webViewPoolManager.acquireWebView(url) ?: return null
        return try {
            val testUrl = injectPayloadIntoUrl(url, payload)
            withContext(Dispatchers.Main) { wrapper.webView.loadUrl(testUrl) }
            val loaded = withTimeoutOrNull(Constants.WEBVIEW_TIMEOUT_MS) {
                waitForPageLoad(wrapper.webView)
            } != null
            if (!loaded) {
                webViewPoolManager.releaseWebView(wrapper)
                return null
            }
            val content = evaluateJs(wrapper.webView, "document.documentElement.outerHTML")
            val responseTime = System.currentTimeMillis() - wrapper.lastUsedTime
            ResponseData(url = testUrl, content = content ?: "", responseTimeMs = responseTime, statusCode = 200)
        } catch (e: Exception) {
            null
        } finally {
            webViewPoolManager.releaseWebView(wrapper)
        }
    }

    suspend fun closeSession() {
        withContext(Dispatchers.IO) {
            currentSession?.let { wrapper ->
                webViewPoolManager.releaseWebView(wrapper)
                currentSession = null
            }
        }
    }

    data class ResponseData(val url: String, val content: String, val responseTimeMs: Long, val statusCode: Int)
}
