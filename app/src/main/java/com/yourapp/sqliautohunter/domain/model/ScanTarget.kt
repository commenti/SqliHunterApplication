package com.yourapp.sqliautohunter.domain.model

data class ScanTarget(
    val url: String,
    val keywordSource: String,
    val priority: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)
