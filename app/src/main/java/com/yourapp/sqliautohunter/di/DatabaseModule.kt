package com.yourapp.sqliautohunter.di

import android.content.Context
import com.yourapp.sqliautohunter.data.local.database.AppDatabase
import com.yourapp.sqliautohunter.data.local.database.dao.CrashLogDao
import com.yourapp.sqliautohunter.data.local.database.dao.SearchQueueDao
import com.yourapp.sqliautohunter.data.local.database.dao.TestedUrlHashDao
import com.yourapp.sqliautohunter.data.local.database.dao.VulnerabilityResultDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideSearchQueueDao(database: AppDatabase): SearchQueueDao {
        return database.searchQueueDao()
    }

    @Provides
    @Singleton
    fun provideTestedUrlHashDao(database: AppDatabase): TestedUrlHashDao {
        return database.testedUrlHashDao()
    }

    @Provides
    @Singleton
    fun provideVulnerabilityResultDao(database: AppDatabase): VulnerabilityResultDao {
        return database.vulnerabilityResultDao()
    }

    @Provides
    @Singleton
    fun provideCrashLogDao(database: AppDatabase): CrashLogDao {
        return database.crashLogDao()
    }
}
