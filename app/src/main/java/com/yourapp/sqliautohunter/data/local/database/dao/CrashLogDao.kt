package com.yourapp.sqliautohunter.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.yourapp.sqliautohunter.data.local.database.entity.CrashLogEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for crash / error diagnostics.
 *
 * Writes must be resilient — the global uncaught-exception handler runs on a
 * dying process, so inserts are best-effort with a short blocking path via
 * runBlocking in the manager (never from DAO itself). Reads are paginated;
 * the Logs screen filters by module and severity and searches message text.
 */
@Dao
interface CrashLogDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: CrashLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(entities: List<CrashLogEntity>): List<Long>

    @Query("SELECT * FROM crash_logs WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): CrashLogEntity?

    @Query(
        """
        SELECT * FROM crash_logs
        ORDER BY timestamp DESC
        LIMIT :limit OFFSET :offset
        """
    )
    fun observePage(limit: Int, offset: Int): Flow<List<CrashLogEntity>>

    @Query(
        """
        SELECT * FROM crash_logs
        WHERE severity = :severity
        ORDER BY timestamp DESC
        LIMIT :limit OFFSET :offset
        """
    )
    fun observeBySeverity(
        severity: String,
        limit: Int,
        offset: Int
    ): Flow<List<CrashLogEntity>>

    @Query(
        """
        SELECT * FROM crash_logs
        WHERE module_name = :module
        ORDER BY timestamp DESC
        LIMIT :limit OFFSET :offset
        """
    )
    fun observeByModule(
        module: String,
        limit: Int,
        offset: Int
    ): Flow<List<CrashLogEntity>>

    @Query(
        """
        SELECT * FROM crash_logs
        WHERE module_name = :module AND severity = :severity
        ORDER BY timestamp DESC
        LIMIT :limit OFFSET :offset
        """
    )
    fun observeByModuleAndSeverity(
        module: String,
        severity: String,
        limit: Int,
        offset: Int
    ): Flow<List<CrashLogEntity>>

    @Query(
        """
        SELECT * FROM crash_logs
        WHERE error_message LIKE '%' || :query || '%'
           OR stack_trace LIKE '%' || :query || '%'
        ORDER BY timestamp DESC
        LIMIT :limit OFFSET :offset
        """
    )
    fun search(query: String, limit: Int, offset: Int): Flow<List<CrashLogEntity>>

    @Query("SELECT DISTINCT module_name FROM crash_logs ORDER BY module_name ASC")
    fun observeDistinctModules(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM crash_logs")
    suspend fun totalCount(): Int

    @Query("SELECT COUNT(*) FROM crash_logs")
    fun observeTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM crash_logs WHERE severity = :severity")
    fun observeCountBySeverity(severity: String): Flow<Int>

    /**
     * Prune oldest rows when the table grows past [Constants.CRASH_LOG_MAX_ROWS].
     * Keeps the newest [keepCount] rows, deletes the rest.
     */
    @Query(
        """
        DELETE FROM crash_logs
        WHERE id NOT IN (
            SELECT id FROM crash_logs
            ORDER BY timestamp DESC
            LIMIT :keepCount
        )
        """
    )
    suspend fun pruneToNewest(keepCount: Int): Int

    @Query("DELETE FROM crash_logs WHERE timestamp < :cutoffEpochMs")
    suspend fun pruneOlderThan(cutoffEpochMs: Long): Int

    @Query("DELETE FROM crash_logs")
    suspend fun clearAll()
}