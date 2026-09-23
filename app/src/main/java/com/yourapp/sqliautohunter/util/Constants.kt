package com.yourapp.sqliautohunter.util

/**
 * Central constants for SQLi Auto-Hunter.
 *
 * Everything tunable lives here — timings, limits, keys, channel IDs,
 * DB names, and the fixed set of string enums used across layers.
 * No magic numbers anywhere else in the codebase.
 */
object Constants {

    // ------------------------------------------------------------------
    // App identity
    // ------------------------------------------------------------------
    const val APP_NAME = "SQLi Auto-Hunter"
    const val DB_NAME = "sqli_auto_hunter.db"
    const val DATASTORE_NAME = "sqli_settings"

    // ------------------------------------------------------------------
    // Notification
    // ------------------------------------------------------------------
    const val NOTIFICATION_CHANNEL_ID = "sqli_scan_channel"
    const val NOTIFICATION_CHANNEL_NAME = "Scan Progress"
    const val NOTIFICATION_CHANNEL_DESC = "Live scan progress and controls"
    const val NOTIFICATION_ID_SCAN = 1001
    const val NOTIFICATION_ID_CRASH = 1002

    const val ACTION_PAUSE_SCAN = "com.yourapp.sqliautohunter.action.PAUSE"
    const val ACTION_RESUME_SCAN = "com.yourapp.sqliautohunter.action.RESUME"
    const val ACTION_STOP_SCAN = "com.yourapp.sqliautohunter.action.STOP"

    const val SERVICE_NOTIF_TITLE = "SQLi Auto-Hunter running"
    const val SERVICE_NOTIF_TEXT_IDLE = "Idle — waiting for targets"

    // ------------------------------------------------------------------
    // Service / WorkManager
    // ------------------------------------------------------------------
    const val SERVICE_RESTART_DELAY_MS = 5_000L
    const val WORKER_TAG_SCAN = "sqli_scan_worker"
    const val WORKER_NAME_PERIODIC_RESCUE = "sqli_periodic_rescue"
    const val WORKER_INPUT_URLS = "input_urls"
    const val WORKER_INPUT_KEYWORD = "input_keyword"
    const val WORKER_RESCUE_INTERVAL_MIN = 15L

    // ------------------------------------------------------------------
    // Queue / batching
    // ------------------------------------------------------------------
    const val QUEUE_BATCH_SIZE = 50
    const val QUEUE_BATCH_SIZE_MAX = 100
    const val PROGRESS_EMIT_INTERVAL_MS = 400L
    const val CSV_FLUSH_EVERY_N_RESULTS = 10

    // ------------------------------------------------------------------
    // Scanner engine
    // ------------------------------------------------------------------
    const val MIN_CONFIRMATIONS_FOR_VULNERABLE = 3
    const val RESPONSE_SNIPPET_MAX_LEN = 512
    const val PAYLOAD_MAX_LEN = 4_096
    const val URL_MAX_LEN = 8_192

    const val DEFAULT_PAGE_TIMEOUT_MS = 15_000L
    const val DEFAULT_LOAD_EVENT_TIMEOUT_MS = 20_000L
    const val DEFAULT_SCAN_DELAY_MS = 250L
    const val DEFAULT_RETRY_COUNT = 3
    const val RETRY_BACKOFF_BASE_MS = 800L
    const val RETRY_BACKOFF_MAX_MS = 20_000L

    const val TIME_BASED_MIN_DELTA_MS = 4_500L
    const val TIME_BASED_SLEEP_SECONDS = 5
    const val BOOLEAN_SIMILARITY_THRESHOLD = 0.92f

    // ------------------------------------------------------------------
    // Search scraper
    // ------------------------------------------------------------------
    const val SEARCH_RESULTS_PER_PAGE = 30
    const val SEARCH_MAX_PAGES_PER_KEYWORD = 3
    const val SEARCH_CIRCUIT_BREAKER_THRESHOLD = 10
    const val SEARCH_CIRCUIT_COOLDOWN_MS = 10 * 60 * 1000L

    // ------------------------------------------------------------------
    // Proxy rotation
    // ------------------------------------------------------------------
    const val PROXY_HEALTH_CHECK_TIMEOUT_MS = 6_000L
    const val PROXY_HEALTH_CHECK_URL = "https://api.ipify.org?format=json"
    const val PROXY_MAX_CONSECUTIVE_FAILURES = 3

    // ------------------------------------------------------------------
    // WebView pool
    // ------------------------------------------------------------------
    const val WEBVIEW_POOL_MIN = 2
    const val WEBVIEW_POOL_MAX = 8
    const val WEBVIEW_IDLE_RECYCLE_MS = 60_000L
    const val WEBVIEW_MAX_PAGES_PER_INSTANCE = 25

    // ------------------------------------------------------------------
    // RAM thresholds (MB)
    // ------------------------------------------------------------------
    const val RAM_TIER_LOW_MB = 3_072L
    const val RAM_TIER_MID_MB = 6_144L
    const val PARALLEL_LOW_RAM = 2
    const val PARALLEL_MID_RAM = 4
    const val PARALLEL_HIGH_RAM = 6
    const val PARALLEL_ULTRA_RAM = 8

    // ------------------------------------------------------------------
    // Crash logging
    // ------------------------------------------------------------------
    const val CRASH_LOG_MAX_ROWS = 5_000
    const val CRASH_STACK_TRACE_MAX_LEN = 16_384
    const val SEVERITY_WARNING = "Warning"
    const val SEVERITY_ERROR = "Error"
    const val SEVERITY_FATAL = "Fatal"

    // ------------------------------------------------------------------
    // URL hash / dedup
    // ------------------------------------------------------------------
    const val HASH_ALGO_SHA256 = "SHA-256"

    // ------------------------------------------------------------------
    // File export
    // ------------------------------------------------------------------
    const val CSV_EXPORT_DIR = "exports"
    const val CSV_FILE_PREFIX = "sqli_results_"
    const val CSV_LOG_PREFIX = "sqli_logs_"
    const val CSV_MIME = "text/csv"
    const val TXT_MIME = "text/plain"
    const val CSV_DATE_PATTERN = "yyyyMMdd_HHmmss"
    const val CSV_COLUMNS = "URL,Vulnerability_Type,Confidence_Score,Payload_Used,Response_Snippet,Discovered_At,Keyword_Source"

    // ------------------------------------------------------------------
    // Dork templates
    // ------------------------------------------------------------------
    val DORK_TEMPLATES: List<String> = listOf(
        "inurl:php?id=",
        "inurl:index.php?id=",
        "inurl:catid=",
        "inurl:product.php?id=",
        "inurl:news.php?id=",
        "inurl:article.php?id=",
        "inurl:item.php?id=",
        "inurl:view.php?id=",
        "inurl:page.php?id=",
        "inurl:display.php?id=",
        "inurl:detail.php?id=",
        "site:.com inurl:catid=",
        "site:.net inurl:product_id=",
        "site:.org inurl:item_id=",
        "inurl:?id=&cat=",
        "inurl:?p=",
        "inurl:?page=",
        "inurl:?view=",
        "inurl:?product="
    )

    // ------------------------------------------------------------------
    // Blacklist — domain suffixes filtered before queue insert
    // ------------------------------------------------------------------
    val DEFAULT_DOMAIN_BLACKLIST: List<String> = listOf(
        ".gov", ".mil", ".edu",
        "bank", "chase.com", "wellsfargo.com", "bankofamerica.com",
        "citibank.com", "hsbc.com", "barclays.com", "santander.com",
        "paypal.com", "stripe.com", "squareup.com"
    )

    // ------------------------------------------------------------------
    // User-agents — matched bundles (UA + sec-ch-ua, not mixed)
    // ------------------------------------------------------------------
    data class UaBundle(
        val userAgent: String,
        val secChUa: String?,
        val secChUaMobile: String?,
        val secChUaPlatform: String?
    )

    val UA_BUNDLES: List<UaBundle> = listOf(
        UaBundle(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36",
            "\"Google Chrome\";v=\"125\", \"Chromium\";v=\"125\", \"Not.A/Brand\";v=\"24\"",
            "?0",
            "\"Windows\""
        ),
        UaBundle(
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
            "\"Google Chrome\";v=\"124\", \"Chromium\";v=\"124\", \"Not.A/Brand\";v=\"24\"",
            "?0",
            "\"macOS\""
        ),
        UaBundle(
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
            "\"Google Chrome\";v=\"124\", \"Chromium\";v=\"124\", \"Not.A/Brand\";v=\"24\"",
            "?0",
            "\"Linux\""
        ),
        UaBundle(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:126.0) Gecko/20100101 Firefox/126.0",
            null, null, null
        ),
        UaBundle(
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_4 like Mac OS X) " +
                "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Mobile/15E148 Safari/604.1",
            null, null, null
        )
    )

    // ------------------------------------------------------------------
    // Search engine endpoints
    // ------------------------------------------------------------------
    const val BING_SEARCH_URL = "https://www.bing.com/search"
    const val DDG_HTML_URL = "https://html.duckduckgo.com/html/"
    const val SEARX_BASE_URL = "https://searx.be/search"

    const val PARAM_Q = "q"
    const val PARAM_FIRST = "first"
    const val PARAM_COUNT = "count"
    const val PARAM_FORMAT = "format"
    const val PARAM_FORMAT_JSON = "json"

    // ------------------------------------------------------------------
    // Timeouts (network)
    // ------------------------------------------------------------------
    const val HTTP_CONNECT_TIMEOUT_SEC = 15L
    const val HTTP_READ_TIMEOUT_SEC = 20L
    const val HTTP_WRITE_TIMEOUT_SEC = 15L
    const val HTTP_CALL_TIMEOUT_SEC = 45L
}