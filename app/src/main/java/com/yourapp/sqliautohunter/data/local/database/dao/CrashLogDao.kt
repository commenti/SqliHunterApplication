package com.yourapp.sqliautohunter.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.yourapp.sqliautohunter.data.local.database.entity.CrashLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CrashLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: CrashLogEntity)

    @Query("SELECT * FROM crash_logs ORDER BY timestamp DESC")
    fun getAll(): Flow<List<CrashLogEntity>>

    @Query("SELECT * FROM crash_logs WHERE moduleName = :module ORDER BY timestamp DESC")
    fun getByModule(module: String): Flow<List<CrashLogEntity>>

    @Query("SELECT * FROM crash_logs WHERE severity = :severity ORDER BY timestamp DESC")
    fun getBySeverity(severity: String): Flow<List<CrashLogEntity>>

    @Query("SELECT * FROM crash_logs WHERE errorMessage LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun search(query: String): Flow<List<CrashLogEntity>>

    @Query("SELECT COUNT(*) FROM crash_logs")
    fun getCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM crash_logs WHERE severity = :severity")
    fun getCountBySeverity(severity: String): Flow<Int>

    @Query("SELECT * FROM crash_logs ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<CrashLogEntity>

    @Query("DELETE FROM crash_logs WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM crash_logs")
    suspend fun deleteAll()

    @Query("SELECT * FROM crash_logs WHERE id = :id")
    suspend fun getById(id: Long): CrashLogEntity?
}
