package com.yourapp.sqliautohunter.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persistent scan queue. One row per URL that has been enqueued for testing.
 *
 * Status values are constrained to [ScanStatus] string constants:
 *   pending | testing | vulnerable | not_vulnerable | error
 *
 * Index on (status, timestamp) supports the hot path: "pull next N pending rows
 * ordered by insertion time" without a full-table scan.
 */
@Entity(
    tableName = "search_queue",
    indices = [
        Index(value = ["status", "timestamp"], name = "idx_queue_status_ts"),
        Index(value = ["url"], name = "idx_queue_url")
    ]
)
data class SearchQueueEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "url")
    val url: String,

    @ColumnInfo(name = "keyword_source")
    val keywordSource: String,

    @ColumnInfo(name = "status")
    val status: String,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long
) {
    companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_TESTING = "testing"
        const val STATUS_VULNERABLE = "vulnerable"
        const val STATUS_NOT_VULNERABLE = "not_vulnerable"
        const val STATUS_ERROR = "error"
    }
}