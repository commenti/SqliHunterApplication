package com.yourapp.sqliautohunter.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.yourapp.sqliautohunter.data.local.database.dao.CrashLogDao
import com.yourapp.sqliautohunter.data.local.database.dao.SearchQueueDao
import com.yourapp.sqliautohunter.data.local.database.dao.TestedUrlHashDao
import com.yourapp.sqliautohunter.data.local.database.dao.VulnerabilityResultDao
import com.yourapp.sqliautohunter.data.local.database.entity.CrashLogEntity
import com.yourapp.sqliautohunter.data.local.database.entity.SearchQueueEntity
import com.yourapp.sqliautohunter.data.local.database.entity.TestedUrlHashEntity
import com.yourapp.sqliautohunter.data.local.database.entity.VulnerabilityResultEntity
import com.yourapp.sqliautohunter.util.Constants

@Database(
    entities = [
        SearchQueueEntity::class,
        TestedUrlHashEntity::class,
        VulnerabilityResultEntity::class,
        CrashLogEntity::class
    ],
    version = Constants.DATABASE_VERSION,
    exportSchema = false
)
@TypeConverters(
    com.yourapp.sqliautohunter.data.local.database.entity.ScanStatusConverter::class,
    com.yourapp.sqliautohunter.data.local.database.entity.VulnerabilityTypeConverter::class,
    com.yourapp.sqliautohunter.data.local.database.entity.ConfidenceLevelConverter::class
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun searchQueueDao(): SearchQueueDao
    abstract fun testedUrlHashDao(): TestedUrlHashDao
    abstract fun vulnerabilityResultDao(): VulnerabilityResultDao
    abstract fun crashLogDao(): CrashLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    Constants.DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun getInMemoryDatabase(context: Context): AppDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                AppDatabase::class.java
            )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
