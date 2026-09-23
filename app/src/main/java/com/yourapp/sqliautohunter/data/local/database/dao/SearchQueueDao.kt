package com.yourapp.sqliautohunter.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.yourapp.sqliautohunter.data.local.database.entity.SearchQueueEntity
import com.yourapp.sqliautohunter.data.local.database.entity.ScanStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchQueueDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(queueItem: SearchQueueEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<SearchQueueEntity>): List<Long>

    @Query("SELECT * FROM search_queue WHERE status = 'PENDING' ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getPending(limit: Int): List<SearchQueueEntity>

    @Query("SELECT * FROM search_queue WHERE status = 'TESTING' ORDER BY timestamp ASC")
    suspend fun getTesting(): List<SearchQueueEntity>

    @Query("SELECT * FROM search_queue WHERE keywordSource = :keyword ORDER BY timestamp ASC")
    fun getByKeyword(keyword: String): Flow<List<SearchQueueEntity>>

    @Query("SELECT * FROM search_queue ORDER BY timestamp DESC")
    fun getAll(): Flow<List<SearchQueueEntity>>

    @Query("SELECT COUNT(*) FROM search_queue WHERE status = 'PENDING'")
    fun getPendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM search_queue WHERE status = 'VULNERABLE'")
    fun getVulnerableCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM search_queue WHERE status = 'TESTING'")
    fun getTestingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM search_queue WHERE status = 'NOT_VULNERABLE'")
    fun getNotVulnerableCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM search_queue WHERE status = 'ERROR'")
    fun getErrorCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM search_queue")
    fun getTotalCount(): Flow<Int>

    @Update
    suspend fun update(queueItem: SearchQueueEntity)

    @Transaction
    suspend fun updateStatus(id: Long, status: ScanStatus) {
        update(SearchQueueEntity(id, "", "", status))
    }

    @Query("UPDATE search_queue SET status = :status WHERE id = :id")
    suspend fun updateStatusDirect(id: Long, status: String)

    @Query("DELETE FROM search_queue WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM search_queue")
    suspend fun deleteAll()

    @Query("SELECT * FROM search_queue WHERE status IN ('PENDING', 'TESTING') ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getActiveQueue(limit: Int): List<SearchQueueEntity>

    @Query("SELECT * FROM search_queue WHERE status = 'PENDING' ORDER BY timestamp ASC")
    suspend fun getAllPending(): List<SearchQueueEntity>
}
