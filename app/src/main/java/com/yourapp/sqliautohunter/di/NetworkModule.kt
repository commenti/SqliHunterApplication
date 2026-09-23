package com.yourapp.sqliautohunter.di

import com.yourapp.sqliautohunter.data.remote.proxy.ProxyHealthChecker
import com.yourapp.sqliautohunter.data.remote.proxy.ProxyRotationManager
import com.yourapp.sqliautohunter.data.remote.search.BingScraper
import com.yourapp.sqliautohunter.data.remote.search.DuckDuckGoScraper
import com.yourapp.sqliautohunter.data.remote.search.SearXScraper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    @Provides
    @Singleton
    fun provideBingScraper(): BingScraper {
        return BingScraper()
    }

    @Provides
    @Singleton
    fun provideDuckDuckGoScraper(): DuckDuckGoScraper {
        return DuckDuckGoScraper()
    }

    @Provides
    @Singleton
    fun provideSearXScraper(): SearXScraper {
        return SearXScraper()
    }

    @Provides
    @Singleton
    fun provideProxyHealthChecker(): ProxyHealthChecker {
        return ProxyHealthChecker()
    }

    @Provides
    @Singleton
    fun provideProxyRotationManager(proxyHealthChecker: ProxyHealthChecker): ProxyRotationManager {
        return ProxyRotationManager(proxyHealthChecker)
    }
}
