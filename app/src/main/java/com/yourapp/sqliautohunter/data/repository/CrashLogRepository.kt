package com.yourapp.sqliautohunter.data.repository

import com.yourapp.sqliautohunter.data.local.database.dao.CrashLogDao
import com.yourapp.sqliautohunter.data.local.database.entity.CrashLogEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class CrashLogRepository(
    private val crashLogDao: CrashLogDao
) {

    fun getAllLogs(): Flow<List<CrashLogEntity>> = crashLogDao.getAll()

    fun getLogsByModule(module: String): Flow<List<CrashLogEntity>> {
        return crashLogDao.getByModule(module)
    }

    fun getLogsBySeverity(severity: String): Flow<List<CrashLogEntity>> {
        return crashLogDao.getBySeverity(severity)
    }

    fun searchLogs(query: String): Flow<List<CrashLogEntity>> {
        return crashLogDao.search(query)
    }

    fun getLogCount(): Flow<Int> = crashLogDao.getCount()

    suspend fun saveLog(log: CrashLogEntity) {
        withContext(Dispatchers.IO) {
            crashLogDao.insert(log)
        }
    }

    suspend fun saveLogs(logs: List<CrashLogEntity>) {
        withContext(Dispatchers.IO) {
            logs.forEach { log ->
                crashLogDao.insert(log)
            }
        }
    }

    suspend fun getRecentLogs(limit: Int): List<CrashLogEntity> {
        return withContext(Dispatchers.IO) {
            crashLogDao.getRecent(limit)
        }
    }

    suspend fun deleteLog(id: Long) {
        withContext(Dispatchers.IO) {
            crashLogDao.delete(id)
        }
    }

    suspend fun clearLogs() {
        withContext(Dispatchers.IO) {
            crashLogDao.deleteAll()
        }
    }

    fun getLogStats(): Flow<LogStats> {
        return combine(
            crashLogDao.getCount(),
            crashLogDao.getCountBySeverity("Fatal"),
            crashLogDao.getCountBySeverity("Error"),
            crashLogDao.getCountBySeverity("Warning")
        ) { total, fatal, error, warning ->
            LogStats(
                total = total,
                bySeverity = mapOf(
                    "Fatal" to fatal,
                    "Error" to error,
                    "Warning" to warning
                )
            )
        }.flowOn(Dispatchers.IO)
    }

    suspend fun getLogById(id: Long): CrashLogEntity? {
        return withContext(Dispatchers.IO) {
            crashLogDao.getById(id)
        }
    }

    data class LogStats(
        val total: Int,
        val bySeverity: Map<String, Int>
    )
}
