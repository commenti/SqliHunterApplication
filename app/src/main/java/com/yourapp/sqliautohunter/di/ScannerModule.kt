package com.yourapp.sqliautohunter.di

import com.yourapp.sqliautohunter.data.local.database.dao.SearchQueueDao
import com.yourapp.sqliautohunter.data.local.database.dao.TestedUrlHashDao
import com.yourapp.sqliautohunter.data.repository.QueueRepository
import com.yourapp.sqliautohunter.data.repository.ScanResultRepository
import com.yourapp.sqliautohunter.domain.usecase.CheckUrlHashUseCase
import com.yourapp.sqliautohunter.domain.usecase.FilterBlacklistedDomainUseCase
import com.yourapp.sqliautohunter.domain.usecase.NormalizeUrlUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ScannerModule {

    @Provides
    @Singleton
    fun provideQueueRepository(
        searchQueueDao: SearchQueueDao,
        checkUrlHashUseCase: CheckUrlHashUseCase,
        filterBlacklistedDomainUseCase: FilterBlacklistedDomainUseCase,
        normalizeUrlUseCase: NormalizeUrlUseCase
    ): QueueRepository {
        return QueueRepository(
            searchQueueDao,
            checkUrlHashUseCase,
            filterBlacklistedDomainUseCase,
            normalizeUrlUseCase
        )
    }

    @Provides
    @Singleton
    fun provideScanResultRepository(
        vulnerabilityResultDao: com.yourapp.sqliautohunter.data.local.database.dao.VulnerabilityResultDao,
        testedUrlHashDao: TestedUrlHashDao
    ): ScanResultRepository {
        return ScanResultRepository(vulnerabilityResultDao, testedUrlHashDao)
    }

    @Provides
    @Singleton
    fun provideCheckUrlHashUseCase(
        testedUrlHashDao: TestedUrlHashDao,
        normalizeUrlUseCase: NormalizeUrlUseCase
    ): CheckUrlHashUseCase {
        return CheckUrlHashUseCase(testedUrlHashDao, normalizeUrlUseCase)
    }
}
