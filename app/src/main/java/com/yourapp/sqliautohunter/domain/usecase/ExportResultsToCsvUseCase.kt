package com.yourapp.sqliautohunter.domain.usecase

import com.yourapp.sqliautohunter.data.local.database.dao.VulnerabilityResultDao
import com.yourapp.sqliautohunter.data.local.database.entity.VulnerabilityResultEntity
import com.yourapp.sqliautohunter.util.CsvWriter
import com.yourapp.sqliautohunter.util.FileStorageHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class ExportResultsToCsvUseCase(
    private val vulnerabilityResultDao: VulnerabilityResultDao,
    private val fileStorageHelper: FileStorageHelper,
    private val csvWriter: CsvWriter
) {

    suspend operator fun invoke(
        fileName: String? = null,
        exportAll: Boolean = true,
        filterByKeyword: String? = null,
        filterByConfidence: String? = null
    ): File? {
        return withContext(Dispatchers.IO) {
            val results = if (exportAll) {
                if (filterByKeyword != null && filterByConfidence != null) {
                    vulnerabilityResultDao.getByKeyword(filterByKeyword).value.filter { 
                        it.confidence.name == filterByConfidence
                    }
                } else if (filterByKeyword != null) {
                    vulnerabilityResultDao.getByKeyword(filterByKeyword).value
                } else if (filterByConfidence != null) {
                    vulnerabilityResultDao.getAll().value.filter { 
                        it.confidence.name == filterByConfidence
                    }
                } else {
                    vulnerabilityResultDao.getAll().value
                }
            } else {
                vulnerabilityResultDao.getRecent(100)
            }

            if (results.isEmpty()) {
                return@withContext null
            }

            val csvFile = fileStorageHelper.createCsvFile(fileName ?: generateFileName())
            val headers = listOf(
                "ID", "URL", "Vulnerability Type", "Payload Used", "Confidence", 
                "Response Snippet", "Discovered At", "Keyword Source"
            )

            val rows = results.map { result ->
                listOf(
                    result.id.toString(),
                    result.url,
                    result.vulnType.name,
                    result.payloadUsed,
                    result.confidence.name,
                    result.responseSnippet.replace("\n", " ").replace("\r", " "),
                    result.discoveredAt.toString(),
                    result.keywordSource
                )
            }

            csvWriter.writeCsv(csvFile, headers, rows)
            csvFile
        }
    }

    private fun generateFileName(): String {
        val timestamp = System.currentTimeMillis()
        return "sqli_results_$timestamp.csv"
    }

    suspend fun exportToDownloads(fileName: String? = null): File? {
        return withContext(Dispatchers.IO) {
            val file = invoke(fileName)
            if (file != null) {
                fileStorageHelper.copyToDownloads(file, fileName ?: file.name)
            }
            file
        }
    }

    suspend fun exportFiltered(
        keyword: String? = null,
        confidence: String? = null,
        fileName: String? = null
    ): File? {
        return invoke(fileName, exportAll = true, filterByKeyword = keyword, filterByConfidence = confidence)
    }

    suspend fun getExportPreview(
        limit: Int = 10,
        keyword: String? = null,
        confidence: String? = null
    ): List<CsvPreviewRow> {
        return withContext(Dispatchers.IO) {
            val results = if (keyword != null && confidence != null) {
                vulnerabilityResultDao.getByKeyword(keyword).value.filter { 
                    it.confidence.name == confidence
                }
            } else if (keyword != null) {
                vulnerabilityResultDao.getByKeyword(keyword).value
            } else if (confidence != null) {
                vulnerabilityResultDao.getAll().value.filter { 
                    it.confidence.name == confidence
                }
            } else {
                vulnerabilityResultDao.getRecent(limit)
            }

            results.take(limit).map { result ->
                CsvPreviewRow(
                    url = result.url,
                    vulnType = result.vulnType.name,
                    payload = result.payloadUsed,
                    confidence = result.confidence.name,
                    discoveredAt = result.discoveredAt
                )
            }
        }
    }

    data class CsvPreviewRow(
        val url: String,
        val vulnType: String,
        val payload: String,
        val confidence: String,
        val discoveredAt: Long
    )
}
