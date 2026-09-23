package com.yourapp.sqliautohunter.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.yourapp.sqliautohunter.data.local.database.entity.TestedUrlHashEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for the deduplication ledger.
 *
 * Race-safety contract: `insertIfAbsent` uses OnConflictStrategy.IGNORE on the
 * PK (url_hash). A return of -1L means the row already existed — caller treats
 * that as "already tested" and surfaces the existing row. Concurrent workers
 * racing on the same normalized URL resolve deterministically: exactly one gets
 * a real rowId, everyone else gets -1L.
 *
 * Do NOT split check + insert across two calls outside a transaction — that
 * reintroduces the race this DAO exists to close.
 */
@Dao
interface TestedUrlHashDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(entity: TestedUrlHashEntity): Long

    @Query("SELECT * FROM tested_urls_hash WHERE url_hash = :hash LIMIT 1")
    suspend fun getByHash(hash: String): TestedUrlHashEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM tested_urls_hash WHERE url_hash = :hash)")
    suspend fun exists(hash: String): Boolean

    @Query("SELECT url_hash FROM tested_urls_hash WHERE url_hash IN (:hashes)")
    suspend fun existingHashes(hashes: List<String>): List<String>

    @Query("SELECT COUNT(*) FROM tested_urls_hash")
    suspend fun totalCount(): Int

    @Query("SELECT COUNT(*) FROM tested_urls_hash WHERE test_result = :result")
    suspend fun countByResult(result: String): Int

    @Query("SELECT COUNT(*) FROM tested_urls_hash WHERE test_result = :result")
    fun observeCountByResult(result: String): Flow<Int>

    @Query(
        """
        SELECT * FROM tested_urls_hash
        ORDER BY tested_at DESC
        LIMIT :limit
        """
    )
    fun observeRecent(limit: Int): Flow<List<TestedUrlHashEntity>>

    @Query("DELETE FROM tested_urls_hash WHERE tested_at < :cutoffEpochMs")
    suspend fun pruneOlderThan(cutoffEpochMs: Long): Int

    @Query("DELETE FROM tested_urls_hash")
    suspend fun clearAll()

    /**
     * Transactional check-and-insert. Returns the inserted rowId (>= 0) if this
     * call won the insert race, or the existing row if it was already present.
     * Callers use `isNew` to decide whether to dispatch the URL to the queue.
     */
    @Transaction
    suspend fun checkAndInsert(entity: TestedUrlHashEntity): CheckAndInsertResult {
        val rowId = insertIfAbsent(entity)
        return if (rowId != -1L) {
            CheckAndInsertResult(isNew = true, existing = null)
        } else {
            CheckAndInsertResult(isNew = false, existing = getByHash(entity.urlHash))
        }
    }

    data class CheckAndInsertResult(
        val isNew: Boolean,
        val existing: TestedUrlHashEntity?
    )
}