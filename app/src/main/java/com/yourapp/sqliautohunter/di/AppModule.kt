package com.yourapp.sqliautohunter.di

import android.content.Context
import com.yourapp.sqliautohunter.SqliHunterApplication
import com.yourapp.sqliautohunter.data.local.preferences.SettingsDataStore
import com.yourapp.sqliautohunter.data.repository.CrashLogRepository
import com.yourapp.sqliautohunter.data.repository.QueueRepository
import com.yourapp.sqliautohunter.data.repository.ScanResultRepository
import com.yourapp.sqliautohunter.data.repository.SettingsRepository
import com.yourapp.sqliautohunter.domain.usecase.CheckUrlHashUseCase
import com.yourapp.sqliautohunter.domain.usecase.ClassifyResultUseCase
import com.yourapp.sqliautohunter.domain.usecase.ExportResultsToCsvUseCase
import com.yourapp.sqliautohunter.domain.usecase.FilterBlacklistedDomainUseCase
import com.yourapp.sqliautohunter.domain.usecase.GenerateDorkQueriesUseCase
import com.yourapp.sqliautohunter.domain.usecase.NormalizeUrlUseCase
import com.yourapp.sqliautohunter.domain.usecase.ScanUrlForVulnerabilityUseCase
import com.yourapp.sqliautohunter.domain.usecase.ScrapeSearchResultsUseCase
import com.yourapp.sqliautohunter.engine.browser.CdpNetworkInterceptor
import com.yourapp.sqliautohunter.engine.browser.CdpSessionController
import com.yourapp.sqliautohunter.engine.browser.PayloadInjector
import com.yourapp.sqliautohunter.engine.browser.WebViewPoolManager
import com.yourapp.sqliautohunter.engine.concurrency.DeviceRamDetector
import com.yourapp.sqliautohunter.engine.concurrency.DynamicConcurrencyController
import com.yourapp.sqliautohunter.engine.concurrency.ScanWorkerPool
import com.yourapp.sqliautohunter.engine.crash.CrashLogManager
import com.yourapp.sqliautohunter.engine.crash.GlobalExceptionHandler
import com.yourapp.sqliautohunter.service.NotificationController
import com.yourapp.sqliautohunter.util.CsvWriter
import com.yourapp.sqliautohunter.util.FileStorageHelper
import com.yourapp.sqliautohunter.util.HashUtils
import com.yourapp.sqliautohunter.util.PermissionHelper
import com.yourapp.sqliautohunter.util.UrlNormalizer
import com.yourapp.sqliautohunter.util.UserAgentProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideApplicationContext(application: SqliHunterApplication): Context = application

    @Provides
    @Singleton
    fun provideCoroutineScope(): CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context): SettingsDataStore {
        return SettingsDataStore(context)
    }

    @Provides
    @Singleton
    fun provideSettingsRepository(@ApplicationContext context: Context): SettingsRepository {
        return SettingsRepository(context)
    }

    // Utilities
    @Provides
    @Singleton
    fun provideHashUtils(): HashUtils = HashUtils

    @Provides
    @Singleton
    fun provideUrlNormalizer(): UrlNormalizer = UrlNormalizer

    @Provides
    @Singleton
    fun provideUserAgentProvider(): UserAgentProvider = UserAgentProvider

    @Provides
    @Singleton
    fun providePermissionHelper(): PermissionHelper = PermissionHelper

    @Provides
    @Singleton
    fun provideFileStorageHelper(@ApplicationContext context: Context): FileStorageHelper {
        return FileStorageHelper(context)
    }

    @Provides
    @Singleton
    fun provideCsvWriter(): CsvWriter = CsvWriter()

    @Provides
    @Singleton
    fun provideNotificationController(): NotificationController = NotificationController

    // Use Cases
    @Provides
    @Singleton
    fun provideGenerateDorkQueriesUseCase(): GenerateDorkQueriesUseCase = GenerateDorkQueriesUseCase()

    @Provides
    @Singleton
    fun provideNormalizeUrlUseCase(): NormalizeUrlUseCase = NormalizeUrlUseCase()

    @Provides
    @Singleton
    fun provideFilterBlacklistedDomainUseCase(
        normalizeUrlUseCase: NormalizeUrlUseCase
    ): FilterBlacklistedDomainUseCase = FilterBlacklistedDomainUseCase(normalizeUrlUseCase)

    @Provides
    @Singleton
    fun provideClassifyResultUseCase(): ClassifyResultUseCase = ClassifyResultUseCase()

    @Provides
    @Singleton
    fun provideScrapeSearchResultsUseCase(
        bingScraper: com.yourapp.sqliautohunter.data.remote.search.BingScraper,
        duckDuckGoScraper: com.yourapp.sqliautohunter.data.remote.search.DuckDuckGoScraper,
        searXScraper: com.yourapp.sqliautohunter.data.remote.search.SearXScraper
    ): ScrapeSearchResultsUseCase = ScrapeSearchResultsUseCase(bingScraper, duckDuckGoScraper, searXScraper)

    @Provides
    @Singleton
    fun provideScanUrlForVulnerabilityUseCase(
        vulnerabilityResultDao: com.yourapp.sqliautohunter.data.local.database.dao.VulnerabilityResultDao
    ): ScanUrlForVulnerabilityUseCase = ScanUrlForVulnerabilityUseCase(vulnerabilityResultDao)

    @Provides
    @Singleton
    fun provideExportResultsToCsvUseCase(
        vulnerabilityResultDao: com.yourapp.sqliautohunter.data.local.database.dao.VulnerabilityResultDao,
        fileStorageHelper: FileStorageHelper,
        csvWriter: CsvWriter
    ): ExportResultsToCsvUseCase = ExportResultsToCsvUseCase(vulnerabilityResultDao, fileStorageHelper, csvWriter)

    // Crash handling
    @Provides
    @Singleton
    fun provideCrashLogRepository(
        crashLogDao: com.yourapp.sqliautohunter.data.local.database.dao.CrashLogDao
    ): CrashLogRepository = CrashLogRepository(crashLogDao)

    @Provides
    @Singleton
    fun provideCrashLogManager(
        crashLogRepository: CrashLogRepository,
        scope: CoroutineScope
    ): CrashLogManager = CrashLogManager(crashLogRepository, scope)

    @Provides
    @Singleton
    fun provideGlobalExceptionHandler(
        @ApplicationContext context: Context,
        crashLogRepository: CrashLogRepository
    ): GlobalExceptionHandler = GlobalExceptionHandler(context, crashLogRepository)

    // Browser engine
    @Provides
    @Singleton
    fun provideWebViewPoolManager(@ApplicationContext context: Context): WebViewPoolManager {
        return WebViewPoolManager(context)
    }

    @Provides
    @Singleton
    fun provideCdpNetworkInterceptor(): CdpNetworkInterceptor = CdpNetworkInterceptor()

    @Provides
    @Singleton
    fun provideCdpSessionController(webViewPoolManager: WebViewPoolManager): CdpSessionController {
        return CdpSessionController(webViewPoolManager)
    }

    @Provides
    @Singleton
    fun providePayloadInjector(cdpSessionController: CdpSessionController): PayloadInjector {
        return PayloadInjector(cdpSessionController)
    }

    // Concurrency
    @Provides
    @Singleton
    fun provideDeviceRamDetector(@ApplicationContext context: Context): DeviceRamDetector {
        return DeviceRamDetector(context)
    }

    @Provides
    @Singleton
    fun provideDynamicConcurrencyController(
        @ApplicationContext context: Context,
        settingsRepository: SettingsRepository,
        deviceRamDetector: DeviceRamDetector
    ): DynamicConcurrencyController {
        return DynamicConcurrencyController(context, settingsRepository, deviceRamDetector)
    }

    @Provides
    @Singleton
    fun provideScanWorkerPool(
        queueRepository: QueueRepository,
        scanUrlForVulnerabilityUseCase: ScanUrlForVulnerabilityUseCase,
        checkUrlHashUseCase: CheckUrlHashUseCase,
        payloadInjector: PayloadInjector,
        dynamicConcurrencyController: DynamicConcurrencyController,
        scope: CoroutineScope
    ): ScanWorkerPool {
        return ScanWorkerPool(
            queueRepository,
            scanUrlForVulnerabilityUseCase,
            checkUrlHashUseCase,
            payloadInjector,
            dynamicConcurrencyController,
            scope
        )
    }
}
