package com.yourapp.sqliautohunter.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.yourapp.sqliautohunter.util.Constants

@Entity(tableName = "search_queue")
@TypeConverters(ScanStatusConverter::class)
data class SearchQueueEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val url: String,
    val keywordSource: String,
    val status: ScanStatus = ScanStatus.PENDING,
    val timestamp: Long = System.currentTimeMillis()
)

enum class ScanStatus {
    PENDING,
    TESTING,
    VULNERABLE,
    NOT_VULNERABLE,
    ERROR
}

class ScanStatusConverter {
    @androidx.room.TypeConverter
    fun fromString(value: String): ScanStatus {
        return when (value) {
            "TESTING" -> ScanStatus.TESTING
            "VULNERABLE" -> ScanStatus.VULNERABLE
            "NOT_VULNERABLE" -> ScanStatus.NOT_VULNERABLE
            "ERROR" -> ScanStatus.ERROR
            else -> ScanStatus.PENDING
        }
    }

    @androidx.room.TypeConverter
    fun toString(status: ScanStatus): String {
        return status.name
    }
}
