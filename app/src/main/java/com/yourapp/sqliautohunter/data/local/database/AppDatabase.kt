package com.yourapp.sqliautohunter.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.yourapp.sqliautohunter.data.local.database.dao.CrashLogDao
import com.yourapp.sqliautohunter.data.local.database.dao.SearchQueueDao
import com.yourapp.sqliautohunter.data.local.database.dao.TestedUrlHashDao
import com.yourapp.sqliautohunter.data.local.database.dao.VulnerabilityResultDao
import com.yourapp.sqliautohunter.data.local.database.entity.CrashLogEntity
import com.yourapp.sqliautohunter.data.local.database.entity.SearchQueueEntity
import com.yourapp.sqliautohunter.data.local.database.entity.TestedUrlHashEntity
import com.yourapp.sqliautohunter.data.local.database.entity.VulnerabilityResultEntity

/**
 * Room database for SQLi Auto-Hunter.
 *
 * Four tables:
 *   - search_queue        : persistent pending/testing queue
 *   - tested_urls_hash    : SHA-256 dedup ledger
 *   - vulnerability_results : confirmed findings
 *   - crash_logs          : diagnostics
 *
 * Constructed via Hilt (DatabaseModule) with:
 *   - foreign keys disabled (tables are independent by design)
 *   - WAL journal mode (concurrent readers during worker writes)
 *   - fallbackToDestructiveMigration OFF in release; migrations must be added
 *     explicitly when bumping `version`.
 */
@Database(
    entities = [
        SearchQueueEntity::class,
        TestedUrlHashEntity::class,
        VulnerabilityResultEntity::class,
        CrashLogEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun searchQueueDao(): SearchQueueDao
    abstract fun testedUrlHashDao(): TestedUrlHashDao
    abstract fun vulnerabilityResultDao(): VulnerabilityResultDao
    abstract fun crashLogDao(): CrashLogDao

    companion object {
        const val DB_NAME = "sqli_auto_hunter.db"
    }
}