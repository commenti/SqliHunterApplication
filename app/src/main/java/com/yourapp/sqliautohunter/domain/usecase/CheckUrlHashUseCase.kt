package com.yourapp.sqliautohunter.domain.usecase

import com.yourapp.sqliautohunter.data.local.database.dao.TestedUrlHashDao
import com.yourapp.sqliautohunter.util.HashUtils
import com.yourapp.sqliautohunter.util.UrlNormalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CheckUrlHashUseCase(
    private val testedUrlHashDao: TestedUrlHashDao,
    private val normalizeUrlUseCase: NormalizeUrlUseCase
) {

    suspend operator fun invoke(url: String): Boolean {
        return withContext(Dispatchers.IO) {
            val normalized = normalizeUrlUseCase(url)
            val hash = HashUtils.sha256(normalized)
            testedUrlHashDao.exists(hash)
        }
    }

    suspend fun checkAndInsert(
        url: String,
        testResult: String,
        payloadTypesTried: String
    ): Boolean {
        return withContext(Dispatchers.IO) {
            val normalized = normalizeUrlUseCase(url)
            val hash = HashUtils.sha256(normalized)
            testedUrlHashDao.checkAndInsert(hash, url, testResult, payloadTypesTried)
        }
    }

    suspend fun getTestResult(url: String): String? {
        return withContext(Dispatchers.IO) {
            val normalized = normalizeUrlUseCase(url)
            val hash = HashUtils.sha256(normalized)
            testedUrlHashDao.getTestResult(hash)
        }
    }

    suspend fun getOriginalUrl(hash: String): String? {
        return withContext(Dispatchers.IO) {
            testedUrlHashDao.getOriginalUrl(hash)
        }
    }

    suspend fun checkBatch(urls: List<String>): Map<String, Boolean> {
        return withContext(Dispatchers.IO) {
            urls.associate { url ->
                val normalized = normalizeUrlUseCase(url)
                val hash = HashUtils.sha256(normalized)
                url to testedUrlHashDao.exists(hash)
            }
        }
    }

    suspend fun checkBatchAndGetMissing(urls: List<String>): List<String> {
        return withContext(Dispatchers.IO) {
            val results = checkBatch(urls)
            urls.filter { url ->
                !results.getOrDefault(url, false)
            }
        }
    }

    suspend fun insertBatch(urls: List<String>, testResult: String, payloadTypesTried: String) {
        withContext(Dispatchers.IO) {
            urls.forEach { url ->
                val normalized = normalizeUrlUseCase(url)
                val hash = HashUtils.sha256(normalized)
                testedUrlHashDao.checkAndInsert(hash, url, testResult, payloadTypesTried)
            }
        }
    }
}
