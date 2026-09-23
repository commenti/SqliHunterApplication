package com.yourapp.sqliautohunter.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Deduplication ledger. One row per unique normalized URL that has been tested.
 *
 * `urlHash` is the SHA-256 hex of the normalized URL (see UrlNormalizer +
 * HashUtils). It is the primary key — inserts use OnConflictStrategy.IGNORE
 * inside a transaction so check-and-insert is race-condition-safe under
 * concurrent workers.
 *
 * `payloadTypesTried` is a JSON array string, e.g. ["error","boolean","time"].
 */
@Entity(
    tableName = "tested_urls_hash",
    indices = [
        Index(value = ["test_result"], name = "idx_hash_result"),
        Index(value = ["tested_at"], name = "idx_hash_tested_at")
    ]
)
data class TestedUrlHashEntity(
    @PrimaryKey
    @ColumnInfo(name = "url_hash")
    val urlHash: String,

    @ColumnInfo(name = "original_url")
    val originalUrl: String,

    @ColumnInfo(name = "test_result")
    val testResult: String,

    @ColumnInfo(name = "tested_at")
    val testedAt: Long,

    @ColumnInfo(name = "payload_types_tried")
    val payloadTypesTried: String
) {
    companion object {
        const val RESULT_VULNERABLE = "vulnerable"
        const val RESULT_NOT_VULNERABLE = "not_vulnerable"
        const val RESULT_ERROR = "error"
    }
}