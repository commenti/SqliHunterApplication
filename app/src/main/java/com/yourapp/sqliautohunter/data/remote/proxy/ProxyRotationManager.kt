package com.yourapp.sqliautohunter.data.remote.proxy

import com.yourapp.sqliautohunter.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.util.concurrent.TimeUnit

class ProxyRotationManager(
    private val proxyHealthChecker: ProxyHealthChecker = ProxyHealthChecker()
) {

    private val proxies = mutableListOf<ProxyConfig>()
    private var currentIndex = 0
    private var lastRotationTime = 0L
    private var consecutiveFailures = 0

    data class ProxyConfig(
        val host: String,
        val port: Int,
        val protocol: String = "http",
        val username: String? = null,
        val password: String? = null,
        var isHealthy: Boolean = true,
        var lastUsed: Long = 0,
        var failureCount: Int = 0
    )

    fun addProxy(host: String, port: Int, protocol: String = "http", username: String? = null, password: String? = null) {
        proxies.add(ProxyConfig(host, port, protocol, username, password))
    }

    fun addProxies(proxyList: List<String>) {
        proxyList.forEach { proxyString ->
            parseProxyString(proxyString)?.let { config ->
                proxies.add(config)
            }
        }
    }

    fun removeProxy(host: String, port: Int) {
        proxies.removeAll { it.host == host && it.port == port }
    }

    fun clearProxies() {
        proxies.clear()
        currentIndex = 0
    }

    fun getProxyCount(): Int = proxies.size

    fun getHealthyProxyCount(): Int = proxies.count { it.isHealthy }

    suspend fun getNextProxy(): ProxyConfig? {
        return withContext(Dispatchers.IO) {
            if (proxies.isEmpty()) return@withContext null

            // Check if we need to rotate due to failures
            if (consecutiveFailures >= Constants.CIRCUIT_BREAKER_FAILURE_THRESHOLD) {
                val now = System.currentTimeMillis()
                if (now - lastRotationTime < Constants.CIRCUIT_BREAKER_COOLDOWN_MS) {
                    return@withContext null // Still in cooldown
                }
                consecutiveFailures = 0
            }

            // Find next healthy proxy
            var attempts = 0
            val maxAttempts = proxies.size

            while (attempts < maxAttempts) {
                val proxy = proxies[currentIndex]
                currentIndex = (currentIndex + 1) % proxies.size
                attempts++

                if (proxy.isHealthy) {
                    // Check if proxy is still healthy
                    if (proxyHealthChecker.checkProxyHealth(proxy)) {
                        proxy.lastUsed = System.currentTimeMillis()
                        proxy.failureCount = 0
                        consecutiveFailures = 0
                        return@withContext proxy
                    } else {
                        proxy.isHealthy = false
                        consecutiveFailures++
                    }
                }
            }

            // All proxies are unhealthy, try to refresh
            refreshProxyHealth()
            consecutiveFailures++
            null
        }
    }

    suspend fun getProxyClient(): OkHttpClient? {
        val proxy = getNextProxy() ?: return null
        
        return OkHttpClient.Builder()
            .connectTimeout(Constants.NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(Constants.NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(Constants.NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .proxy(java.net.Proxy(java.net.Proxy.Type.HTTP, java.net.InetSocketAddress(proxy.host, proxy.port)))
            .build()
    }

    fun markProxyFailed(proxy: ProxyConfig) {
        proxy.failureCount++
        proxy.isHealthy = false
        consecutiveFailures++
    }

    fun markProxySuccess(proxy: ProxyConfig) {
        proxy.failureCount = 0
        proxy.isHealthy = true
        consecutiveFailures = 0
    }

    suspend fun refreshProxyHealth() {
        withContext(Dispatchers.IO) {
            proxies.forEach { proxy ->
                proxy.isHealthy = proxyHealthChecker.checkProxyHealth(proxy)
            }
        }
    }

    private fun parseProxyString(proxyString: String): ProxyConfig? {
        val parts = proxyString.split(":")
        if (parts.size < 2) return null

        val host = parts[0]
        val port = parts[1].toIntOrNull() ?: return null
        val protocol = if (parts.size >= 3) parts[2] else "http"

        return ProxyConfig(host, port, protocol)
    }

    fun formatProxyString(proxy: ProxyConfig): String {
        return "${proxy.protocol}://${proxy.host}:${proxy.port}"
    }

    fun getProxyList(): List<ProxyConfig> = proxies.toList()

    fun getProxyStrings(): List<String> = proxies.map { formatProxyString(it) }

    fun setProxyList(proxyStrings: List<String>) {
        clearProxies()
        addProxies(proxyStrings)
    }
}
