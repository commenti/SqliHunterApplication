package com.yourapp.sqliautohunter.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tested_urls_hash")
data class TestedUrlHashEntity(
    @PrimaryKey
    val urlHash: String,
    val originalUrl: String,
    val testResult: String,
    val testedAt: Long = System.currentTimeMillis(),
    val payloadTypesTried: String
)
