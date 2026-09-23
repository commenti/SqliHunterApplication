package com.yourapp.sqliautohunter.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persisted crash / error diagnostics. Written by CrashLogManager before the
 * exception is allowed to propagate (Fatal) or swallowed (Warning / Error).
 *
 * severity: Warning | Error | Fatal  (see Constants.SEVERITY_*)
 * url_being_processed is nullable — not every failure is URL-scoped.
 */
@Entity(
    tableName = "crash_logs",
    indices = [
        Index(value = ["timestamp"], name = "idx_crash_ts"),
        Index(value = ["severity"], name = "idx_crash_severity"),
        Index(value = ["module_name"], name = "idx_crash_module")
    ]
)
data class CrashLogEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "module_name")
    val moduleName: String,

    @ColumnInfo(name = "error_message")
    val errorMessage: String,

    @ColumnInfo(name = "stack_trace")
    val stackTrace: String,

    @ColumnInfo(name = "url_being_processed")
    val urlBeingProcessed: String? = null,

    @ColumnInfo(name = "severity")
    val severity: String
)