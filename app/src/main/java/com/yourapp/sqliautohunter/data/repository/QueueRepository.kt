package com.yourapp.sqliautohunter.data.repository

import com.yourapp.sqliautohunter.data.local.database.dao.SearchQueueDao
import com.yourapp.sqliautohunter.data.local.database.entity.SearchQueueEntity
import com.yourapp.sqliautohunter.domain.model.ScanStatus
import com.yourapp.sqliautohunter.domain.usecase.CheckUrlHashUseCase
import com.yourapp.sqliautohunter.domain.usecase.FilterBlacklistedDomainUseCase
import com.yourapp.sqliautohunter.domain.usecase.NormalizeUrlUseCase
import com.yourapp.sqliautohunter.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class QueueRepository(
    private val searchQueueDao: SearchQueueDao,
    private val checkUrlHashUseCase: CheckUrlHashUseCase,
    private val filterBlacklistedDomainUseCase: FilterBlacklistedDomainUseCase,
    private val normalizeUrlUseCase: NormalizeUrlUseCase
) {

    fun getQueueCount(): Flow<Int> = searchQueueDao.getTotalCount()

    fun getPendingCount(): Flow<Int> = searchQueueDao.getPendingCount()

    fun getTestingCount(): Flow<Int> = searchQueueDao.getTestingCount()

    fun getVulnerableCount(): Flow<Int> = searchQueueDao.getVulnerableCount()

    fun getNotVulnerableCount(): Flow<Int> = searchQueueDao.getNotVulnerableCount()

    fun getErrorCount(): Flow<Int> = searchQueueDao.getErrorCount()

    fun getAllQueueItems(): Flow<List<SearchQueueEntity>> = searchQueueDao.getAll()

    fun getQueueStats(): Flow<QueueStats> {
        return combine(
            searchQueueDao.getTotalCount(),
            searchQueueDao.getPendingCount(),
            searchQueueDao.getTestingCount(),
            searchQueueDao.getVulnerableCount(),
            searchQueueDao.getNotVulnerableCount(),
            searchQueueDao.getErrorCount()
        ) { args: Array<Int> ->
            QueueStats(total = args[0], pending = args[1], testing = args[2], vulnerable = args[3], notVulnerable = args[4], error = args[5])
        }.flowOn(Dispatchers.IO)
    }

    suspend fun addToQueue(urls: List<String>, keywordSource: String): Int {
        return withContext(Dispatchers.IO) {
            val filteredUrls = filterBlacklistedDomainUseCase.filterAndNormalize(urls)
            val newUrls = checkUrlHashUseCase.checkBatchAndGetMissing(filteredUrls)
            
            if (newUrls.isEmpty()) return@withContext 0

            val entities = newUrls.map { url ->
                SearchQueueEntity(
                    url = url,
                    keywordSource = keywordSource,
                    status = ScanStatus.PENDING
                )
            }

            val inserted = searchQueueDao.insertAll(entities)
            inserted.size
        }
    }

    suspend fun addToQueueSingle(url: String, keywordSource: String): Boolean {
        return withContext(Dispatchers.IO) {
            val filteredUrl = filterBlacklistedDomainUseCase.filterAndNormalize(listOf(url)).firstOrNull()
            if (filteredUrl == null) return@withContext false

            val alreadyExists = checkUrlHashUseCase(filteredUrl)
            if (alreadyExists) return@withContext false

            val entity = SearchQueueEntity(
                url = filteredUrl,
                keywordSource = keywordSource,
                status = ScanStatus.PENDING
            )

            searchQueueDao.insert(entity) > 0
        }
    }

    suspend fun getNextBatch(batchSize: Int = Constants.BATCH_SIZE): List<SearchQueueEntity> {
        return withContext(Dispatchers.IO) {
            searchQueueDao.getPending(batchSize)
        }
    }

    suspend fun markAsTesting(id: Long) {
        withContext(Dispatchers.IO) {
            searchQueueDao.updateStatus(id, ScanStatus.TESTING)
        }
    }

    suspend fun markAsTesting(entities: List<SearchQueueEntity>) {
        withContext(Dispatchers.IO) {
            entities.forEach { entity ->
                searchQueueDao.updateStatus(entity.id, ScanStatus.TESTING)
            }
        }
    }

    suspend fun markAsVulnerable(id: Long) {
        withContext(Dispatchers.IO) {
            searchQueueDao.updateStatus(id, ScanStatus.VULNERABLE)
        }
    }

    suspend fun markAsNotVulnerable(id: Long) {
        withContext(Dispatchers.IO) {
            searchQueueDao.updateStatus(id, ScanStatus.NOT_VULNERABLE)
        }
    }

    suspend fun markAsError(id: Long) {
        withContext(Dispatchers.IO) {
            searchQueueDao.updateStatus(id, ScanStatus.ERROR)
        }
    }

    suspend fun updateStatus(id: Long, status: ScanStatus) {
        withContext(Dispatchers.IO) {
            searchQueueDao.updateStatus(id, status)
        }
    }

    suspend fun clearQueue() {
        withContext(Dispatchers.IO) {
            searchQueueDao.deleteAll()
        }
    }

    suspend fun removeFromQueue(id: Long) {
        withContext(Dispatchers.IO) {
            searchQueueDao.delete(id)
        }
    }

    suspend fun getByKeyword(keyword: String): Flow<List<SearchQueueEntity>> {
        return searchQueueDao.getByKeyword(keyword)
    }

    suspend fun getActiveQueueItems(limit: Int = Constants.BATCH_SIZE): List<SearchQueueEntity> {
        return withContext(Dispatchers.IO) {
            searchQueueDao.getActiveQueue(limit)
        }
    }

    data class QueueStats(
        val total: Int,
        val pending: Int,
        val testing: Int,
        val vulnerable: Int,
        val notVulnerable: Int,
        val error: Int
    )
}
