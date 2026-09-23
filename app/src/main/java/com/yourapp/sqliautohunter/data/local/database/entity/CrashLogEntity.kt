package com.yourapp.sqliautohunter.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(tableName = "crash_logs")
data class CrashLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val moduleName: String,
    val errorMessage: String,
    val stackTrace: String,
    val urlBeingProcessed: String? = null,
    val severity: String
)
