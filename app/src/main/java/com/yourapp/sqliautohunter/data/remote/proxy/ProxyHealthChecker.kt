package com.yourapp.sqliautohunter.data.remote.proxy

import com.yourapp.sqliautohunter.util.Constants
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.util.concurrent.TimeUnit

class ProxyHealthChecker {

    private val testUrl = "https://httpbin.org/ip"
    private val timeoutMs = 5000L

    suspend fun checkProxyHealth(proxyConfig: ProxyRotationManager.ProxyConfig): Boolean {
        val client = createProxyClient(proxyConfig)
        
        return try {
            val request = Request.Builder()
                .url(testUrl)
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    private fun createProxyClient(proxyConfig: ProxyRotationManager.ProxyConfig): OkHttpClient {
        val proxyType = when (proxyConfig.protocol.lowercase()) {
            "http" -> java.net.Proxy.Type.HTTP
            "https" -> java.net.Proxy.Type.HTTP
            "socks", "socks4", "socks5" -> java.net.Proxy.Type.SOCKS
            else -> java.net.Proxy.Type.HTTP
        }

        val proxy = java.net.Proxy(proxyType, java.net.InetSocketAddress(proxyConfig.host, proxyConfig.port))

        return OkHttpClient.Builder()
            .connectTimeout(timeoutMs, TimeUnit.MILLISECONDS)
            .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
            .writeTimeout(timeoutMs, TimeUnit.MILLISECONDS)
            .proxy(proxy)
            .build()
    }

    suspend fun checkProxyList(proxyList: List<ProxyRotationManager.ProxyConfig>): List<Boolean> {
        return proxyList.map { proxyConfig ->
            checkProxyHealth(proxyConfig)
        }
    }

    suspend fun getHealthyProxies(proxyList: List<ProxyRotationManager.ProxyConfig>): List<ProxyRotationManager.ProxyConfig> {
        val results = checkProxyList(proxyList)
        return proxyList.filterIndexed { index, _ -> results.getOrNull(index) ?: false }
    }

    fun isProxyStringValid(proxyString: String): Boolean {
        val parts = proxyString.split(":")
        if (parts.size < 2) return false
        
        val port = parts[1].toIntOrNull() ?: return false
        return port in 1..65535
    }

    fun validateProxyFormat(proxyString: String): String? {
        val parts = proxyString.split(":")
        if (parts.size < 2) return "Invalid format. Expected: host:port or protocol://host:port"
        
        val port = parts[1].toIntOrNull()
        if (port == null || port !in 1..65535) return "Invalid port number. Must be between 1 and 65535"
        
        return null // Valid
    }
}
