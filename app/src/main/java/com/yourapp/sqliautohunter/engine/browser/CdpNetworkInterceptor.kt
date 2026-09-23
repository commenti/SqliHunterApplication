package com.yourapp.sqliautohunter.engine.browser

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewAssetLoader
import com.yourapp.sqliautohunter.domain.payload.ErrorBasedDetector
import com.yourapp.sqliautohunter.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class CdpNetworkInterceptor {

    private val errorDetector = ErrorBasedDetector()
    private val requestChannel = Channel<InterceptedRequest>(Channel.UNLIMITED)
    private val responseChannel = Channel<InterceptedResponse>(Channel.UNLIMITED)

    val requestFlow: Flow<InterceptedRequest> = requestChannel.receiveAsFlow()
    val responseFlow: Flow<InterceptedResponse> = responseChannel.receiveAsFlow()

    fun setupInterception(webView: WebView): WebViewClient {
        return object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView?, request: android.webkit.WebResourceRequest?): android.webkit.WebResourceResponse? {
                request?.let { webRequest ->
                    val interceptedRequest = InterceptedRequest(
                        url = webRequest.url.toString(),
                        method = webRequest.method,
                        headers = webRequest.requestHeaders.toMap(),
                        isRedirect = webRequest.isRedirect,
                        isForMainFrame = webRequest.isForMainFrame
                    )
                    
                    // Send to channel for processing
                    kotlinx.coroutines.runBlocking {
                        requestChannel.send(interceptedRequest)
                    }
                }
                
                return null // Let WebView handle the request
            }

            override fun onLoadResource(view: WebView?, url: String?) {
                super.onLoadResource(view, url)
                url?.let { requestUrl ->
                    val request = InterceptedRequest(
                        url = requestUrl,
                        method = "GET",
                        headers = emptyMap(),
                        isRedirect = false,
                        isForMainFrame = false
                    )
                    
                    kotlinx.coroutines.runBlocking {
                        requestChannel.send(request)
                    }
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                url?.let { pageUrl ->
                    // Capture the final page state
                    view?.let { webView ->
                        kotlinx.coroutines.runBlocking {
                            capturePageState(webView, pageUrl)
                        }
                    }
                }
            }
        }
    }

    private suspend fun capturePageState(webView: WebView, url: String) {
        try {
            val content: String? = withContext(Dispatchers.Main) {
                suspendCoroutine { cont ->
                    try {
                        webView.evaluateJavascript("document.documentElement.outerHTML") { result ->
                            cont.resume(result)
                        }
                    } catch (e: Exception) {
                        cont.resume(null as String?)
                    }
                }
            }
            val safeContent = content ?: ""
            val response = InterceptedResponse(
                url = url,
                content = safeContent,
                contentLength = safeContent.length,
                isErrorPage = errorDetector.isSqlErrorPage(safeContent)
            )
            responseChannel.send(response)
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun interceptRequest(url: String, method: String = "GET", headers: Map<String, String> = emptyMap()): InterceptedRequest {
        val request = InterceptedRequest(
            url = url,
            method = method,
            headers = headers,
            isRedirect = false,
            isForMainFrame = true
        )
        
        kotlinx.coroutines.runBlocking {
            requestChannel.send(request)
        }
        
        return request
    }

    fun interceptResponse(url: String, content: String, statusCode: Int = 200): InterceptedResponse {
        val response = InterceptedResponse(
            url = url,
            content = content,
            contentLength = content.length,
            isErrorPage = errorDetector.isSqlErrorPage(content)
        )
        
        kotlinx.coroutines.runBlocking {
            responseChannel.send(response)
        }
        
        return response
    }

    suspend fun waitForResponse(url: String, timeoutMs: Long = Constants.WEBVIEW_TIMEOUT_MS): InterceptedResponse? {
        return withContext(Dispatchers.IO) {
            var count = 0
            while (count < 10) {
                val response = responseChannel.tryReceive().getOrNull()
                if (response != null && response.url == url) {
                    return@withContext response
                }
                delay(100)
                count++
            }
            null
        }
    }

    fun close() {
        requestChannel.close()
        responseChannel.close()
    }

    data class InterceptedRequest(
        val url: String,
        val method: String,
        val headers: Map<String, String>,
        val isRedirect: Boolean,
        val isForMainFrame: Boolean,
        val timestamp: Long = System.currentTimeMillis()
    )

    data class InterceptedResponse(
        val url: String,
        val content: String,
        val contentLength: Int,
        val isErrorPage: Boolean,
        val timestamp: Long = System.currentTimeMillis()
    )
}
