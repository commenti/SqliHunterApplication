package com.yourapp.sqliautohunter.data.repository

import com.yourapp.sqliautohunter.data.local.database.dao.TestedUrlHashDao
import com.yourapp.sqliautohunter.data.local.database.dao.VulnerabilityResultDao
import com.yourapp.sqliautohunter.data.local.database.entity.TestedUrlHashEntity
import com.yourapp.sqliautohunter.data.local.database.entity.VulnerabilityResultEntity
import com.yourapp.sqliautohunter.domain.model.ConfidenceLevel
import com.yourapp.sqliautohunter.domain.model.VulnerabilityType
import com.yourapp.sqliautohunter.util.HashUtils
import com.yourapp.sqliautohunter.util.UrlNormalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class ScanResultRepository(
    private val vulnerabilityResultDao: VulnerabilityResultDao,
    private val testedUrlHashDao: TestedUrlHashDao
) {

    fun getAllResults(): Flow<List<VulnerabilityResultEntity>> = vulnerabilityResultDao.getAll()

    fun getResultsCount(): Flow<Int> = vulnerabilityResultDao.getCount()

    fun getResultsByType(type: VulnerabilityType): Flow<List<VulnerabilityResultEntity>> {
        return vulnerabilityResultDao.getByType(type)
    }

    fun getResultsByConfidence(confidence: ConfidenceLevel): Flow<List<VulnerabilityResultEntity>> {
        return vulnerabilityResultDao.getByConfidence(confidence)
    }

    fun getResultsByKeyword(keyword: String): Flow<List<VulnerabilityResultEntity>> {
        return vulnerabilityResultDao.getByKeyword(keyword)
    }

    fun searchResults(query: String): Flow<List<VulnerabilityResultEntity>> {
        return vulnerabilityResultDao.search(query)
    }

    suspend fun saveResult(result: VulnerabilityResultEntity): Long {
        return withContext(Dispatchers.IO) {
            vulnerabilityResultDao.insert(result)
        }
    }

    suspend fun saveResults(results: List<VulnerabilityResultEntity>): List<Long> {
        return withContext(Dispatchers.IO) {
            vulnerabilityResultDao.insertAll(results)
        }
    }

    suspend fun getRecentResults(limit: Int): List<VulnerabilityResultEntity> {
        return withContext(Dispatchers.IO) {
            vulnerabilityResultDao.getRecent(limit)
        }
    }

    suspend fun deleteResult(id: Long) {
        withContext(Dispatchers.IO) {
            vulnerabilityResultDao.delete(id)
        }
    }

    suspend fun clearResults() {
        withContext(Dispatchers.IO) {
            vulnerabilityResultDao.deleteAll()
        }
    }

    suspend fun markUrlAsTested(
        url: String,
        testResult: String,
        payloadTypesTried: String
    ): Boolean {
        return withContext(Dispatchers.IO) {
            val normalized = UrlNormalizer.normalize(url)
            val hash = HashUtils.sha256(normalized)
            testedUrlHashDao.checkAndInsert(hash, url, testResult, payloadTypesTried)
        }
    }

    suspend fun isUrlTested(url: String): Boolean {
        return withContext(Dispatchers.IO) {
            val normalized = UrlNormalizer.normalize(url)
            val hash = HashUtils.sha256(normalized)
            testedUrlHashDao.exists(hash)
        }
    }

    suspend fun getTestResultForUrl(url: String): String? {
        return withContext(Dispatchers.IO) {
            val normalized = UrlNormalizer.normalize(url)
            val hash = HashUtils.sha256(normalized)
            testedUrlHashDao.getTestResult(hash)
        }
    }

    suspend fun getTestedUrlCount(): Int {
        return withContext(Dispatchers.IO) {
            testedUrlHashDao.getCount()
        }
    }

    suspend fun getTestedUrls(): List<TestedUrlHashEntity> {
        return withContext(Dispatchers.IO) {
            testedUrlHashDao.getAll()
        }
    }

    suspend fun clearTestedUrls() {
        withContext(Dispatchers.IO) {
            testedUrlHashDao.deleteAll()
        }
    }

    fun getResultStats(): Flow<ResultStats> {
        return combine(
            vulnerabilityResultDao.getCount(),
            vulnerabilityResultDao.getCountByType("ERROR_BASED"),
            vulnerabilityResultDao.getCountByType("BOOLEAN_BASED"),
            vulnerabilityResultDao.getCountByType("TIME_BASED"),
            vulnerabilityResultDao.getCountByType("UNION_BASED"),
            vulnerabilityResultDao.getCountByConfidence("HIGH"),
            vulnerabilityResultDao.getCountByConfidence("MEDIUM"),
            vulnerabilityResultDao.getCountByConfidence("LOW")
        ) { total, errorBased, booleanBased, timeBased, unionBased, high, medium, low ->
            ResultStats(
                total = total,
                byType = mapOf(
                    "ERROR_BASED" to errorBased,
                    "BOOLEAN_BASED" to booleanBased,
                    "TIME_BASED" to timeBased,
                    "UNION_BASED" to unionBased
                ),
                byConfidence = mapOf(
                    "HIGH" to high,
                    "MEDIUM" to medium,
                    "LOW" to low
                )
            )
        }.flowOn(Dispatchers.IO)
    }

    data class ResultStats(
        val total: Int,
        val byType: Map<String, Int>,
        val byConfidence: Map<String, Int>
    )
}
