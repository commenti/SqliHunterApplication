package com.yourapp.sqliautohunter.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.yourapp.sqliautohunter.data.local.database.entity.TestedUrlHashEntity

@Dao
interface TestedUrlHashDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(hashEntity: TestedUrlHashEntity)

    @Query("SELECT * FROM tested_urls_hash WHERE urlHash = :hash")
    suspend fun getByHash(hash: String): TestedUrlHashEntity?

    @Query("SELECT * FROM tested_urls_hash WHERE originalUrl = :url")
    suspend fun getByUrl(url: String): TestedUrlHashEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM tested_urls_hash WHERE urlHash = :hash)")
    suspend fun exists(hash: String): Boolean

    @Query("SELECT originalUrl FROM tested_urls_hash WHERE urlHash = :hash")
    suspend fun getOriginalUrl(hash: String): String?

    @Query("SELECT testResult FROM tested_urls_hash WHERE urlHash = :hash")
    suspend fun getTestResult(hash: String): String?

    @Transaction
    suspend fun checkAndInsert(hash: String, originalUrl: String, testResult: String, payloadTypesTried: String): Boolean {
        val exists = exists(hash)
        if (!exists) {
            insert(TestedUrlHashEntity(hash, originalUrl, testResult, System.currentTimeMillis(), payloadTypesTried))
            return true
        }
        return false
    }

    @Query("DELETE FROM tested_urls_hash WHERE urlHash = :hash")
    suspend fun delete(hash: String)

    @Query("DELETE FROM tested_urls_hash")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM tested_urls_hash")
    suspend fun getCount(): Int

    @Query("SELECT * FROM tested_urls_hash ORDER BY testedAt DESC")
    suspend fun getAll(): List<TestedUrlHashEntity>

    @Query("SELECT * FROM tested_urls_hash WHERE testResult = 'vulnerable' ORDER BY testedAt DESC")
    suspend fun getAllVulnerable(): List<TestedUrlHashEntity>
}
