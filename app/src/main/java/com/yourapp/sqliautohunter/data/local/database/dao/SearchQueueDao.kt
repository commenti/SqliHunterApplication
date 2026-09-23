package com.yourapp.sqliautohunter.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.yourapp.sqliautohunter.data.local.database.entity.SearchQueueEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for the persistent scan queue.
 *
 * Hot path: `claimNextPending(limit)` inside a transaction — pulls the oldest
 * N pending rows and flips them to `testing` so concurrent workers never pick
 * the same row twice. Batch size is controlled by the caller
 * (Constants.QUEUE_BATCH_SIZE) — never load the entire queue into RAM.
 */
@Dao
interface SearchQueueDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: SearchQueueEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(entities: List<SearchQueueEntity>): List<Long>

    @Update
    suspend fun update(entity: SearchQueueEntity)

    @Query("SELECT * FROM search_queue WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): SearchQueueEntity?

    @Query(
        """
        SELECT * FROM search_queue
        WHERE status = :status
        ORDER BY timestamp ASC
        LIMIT :limit
        """
    )
    suspend fun peekByStatus(status: String, limit: Int): List<SearchQueueEntity>

    /**
     * Atomically claim up to [limit] pending rows for testing.
     * Returns the claimed rows so the caller can dispatch them to workers.
     */
    @Transaction
    suspend fun claimNextPending(limit: Int): List<SearchQueueEntity> {
        val batch = peekByStatus(SearchQueueEntity.STATUS_PENDING, limit)
        if (batch.isEmpty()) return emptyList()
        val claimed = batch.map { it.copy(status = SearchQueueEntity.STATUS_TESTING) }
        updateAll(claimed)
        return claimed
    }

    @Update
    suspend fun updateAll(entities: List<SearchQueueEntity>)

    @Query("UPDATE search_queue SET status = :status WHERE id = :id")
    suspend fun setStatus(id: Long, status: String)

    @Query("UPDATE search_queue SET status = :status WHERE id IN (:ids)")
    suspend fun setStatusBatch(ids: List<Long>, status: String)

    @Query("SELECT COUNT(*) FROM search_queue WHERE status = :status")
    suspend fun countByStatus(status: String): Int

    @Query("SELECT COUNT(*) FROM search_queue WHERE status = :status")
    fun observeCountByStatus(status: String): Flow<Int>

    @Query("SELECT * FROM search_queue WHERE status = :status ORDER BY timestamp ASC LIMIT :limit")
    fun observeByStatus(status: String, limit: Int): Flow<List<SearchQueueEntity>>

    @Query("SELECT * FROM search_queue ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<SearchQueueEntity>>

    @Query("SELECT COUNT(*) FROM search_queue")
    suspend fun totalCount(): Int

    @Query("SELECT COUNT(*) FROM search_queue")
    fun observeTotalCount(): Flow<Int>

    /**
     * On service restart: any row stuck in `testing` from a previous process
     * gets reset to `pending` so it can be retried.
     */
    @Query("UPDATE search_queue SET status = 'pending' WHERE status = 'testing'")
    suspend fun requeueStuckTesting(): Int

    @Query("DELETE FROM search_queue WHERE status IN (:statuses)")
    suspend fun deleteByStatuses(statuses: List<String>): Int

    @Query("DELETE FROM search_queue")
    suspend fun clearAll()
}