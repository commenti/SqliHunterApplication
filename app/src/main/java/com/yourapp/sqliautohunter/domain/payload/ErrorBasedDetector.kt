package com.yourapp.sqliautohunter.domain.payload

import com.yourapp.sqliautohunter.domain.model.VulnerabilityType
import java.util.Locale
import java.util.regex.Pattern

/**
 * Error-based SQLi detector.
 *
 * Strategy: send a single breaking payload, then scan the response body for
 * database-engine-specific error strings. A hit means the backend is reflecting
 * an unhandled DB error — the parameter is reaching a query string raw.
 *
 * Detection is substring + regex based against a curated signature set. The
 * signature list is deliberately tight: only strings that are essentially
 * unique to DB engines survive, because false positives poison the confirmation
 * pipeline downstream.
 *
 * The detector is stateless and thread-safe. One instance per process is fine.
 */
class ErrorBasedDetector {

    /**
     * Result of a single error-based probe.
     *
     * `matchedSignatures` is the list of engine-specific strings found in the
     * response — used by the classifier to weight the confidence and to label
     * the backend engine in the CSV export.
     */
    data class Detection(
        val isHit: Boolean,
        val matchedSignatures: List<String>,
        val suspectedEngine: DbEngine,
        val excerpt: String
    )

    enum class DbEngine(val wire: String) {
        MYSQL("mysql"),
        POSTGRES("postgres"),
        MSSQL("mssql"),
        ORACLE("oracle"),
        SQLITE("sqlite"),
        UNKNOWN("unknown")
    }

    /**
     * Inspect a response body for DB-error signatures.
     *
     * @param body         Raw response text (already decoded as UTF-8).
     * @param baselineBody Optional control response (same URL, no payload).
     *                     When provided, any signature already present in the
     *                     baseline is ignored — prevents counting a site's own
     *                     static "SQL error" help page as a hit.
     */
    fun detect(body: String?, baselineBody: String? = null): Detection {
        if (body.isNullOrEmpty()) {
            return Detection(false, emptyList(), DbEngine.UNKNOWN, "")
        }

        val lower = body.lowercase(Locale.ROOT)
        val baselineLower = baselineBody?.lowercase(Locale.ROOT).orEmpty()

        val hits = mutableListOf<String>()
        var engine = DbEngine.UNKNOWN

        for (sig in SIGNATURES) {
            if (!lower.contains(sig.needle)) continue
            if (baselineLower.isNotEmpty() && baselineLower.contains(sig.needle)) continue
            hits += sig.needle
            if (engine == DbEngine.UNKNOWN || sig.engine != DbEngine.UNKNOWN) {
                engine = sig.engine
            }
        }

        for (rx in REGEX_SIGNATURES) {
            val m = rx.pattern.matcher(body)
            if (!m.find()) continue
            if (baselineBody != null && rx.pattern.matcher(baselineBody).find()) continue
            hits += m.group()
            if (engine == DbEngine.UNKNOWN) engine = rx.engine
        }

        if (hits.isEmpty()) {
            return Detection(false, emptyList(), DbEngine.UNKNOWN, "")
        }

        return Detection(
            isHit = true,
            matchedSignatures = hits.distinct(),
            suspectedEngine = engine,
            excerpt = excerptAround(body, hits.first())
        )
    }

    /** Technique this detector owns. */
    val technique: VulnerabilityType = VulnerabilityType.ERROR_BASED

    /** Payloads this detector typically drives. */
    fun payloads(cap: Int = 6): List<PayloadTemplate> =
        SqliPayloadTemplates.forTechnique(VulnerabilityType.ERROR_BASED, cap)

    // ------------------------------------------------------------------
    // Signature tables
    // ------------------------------------------------------------------

    private data class Signature(val engine: DbEngine, val needle: String)
    private data class RegexSignature(val engine: DbEngine, val pattern: Pattern)

    private companion object {
        val SIGNATURES: List<Signature> = listOf(
            // MySQL / MariaDB
            Signature(DbEngine.MYSQL, "you have an error in your sql syntax"),
            Signature(DbEngine.MYSQL, "warning: mysql_"),
            Signature(DbEngine.MYSQL, "warning: mysqli_"),
            Signature(DbEngine.MYSQL, "unclosed quotation mark after the character string"),
            Signature(DbEngine.MYSQL, "mysql_fetch_array()"),
            Signature(DbEngine.MYSQL, "mysql_num_rows()"),
            Signature(DbEngine.MYSQL, "supplied argument is not a valid mysql"),
            Signature(DbEngine.MYSQL, "check the manual that corresponds to your mysql"),

            // PostgreSQL
            Signature(DbEngine.POSTGRES, "pg_query()"),
            Signature(DbEngine.POSTGRES, "pg_exec()"),
            Signature(DbEngine.POSTGRES, "postgresql query failed"),
            Signature(DbEngine.POSTGRES, "unterminated quoted string at or near"),
            Signature(DbEngine.POSTGRES, "syntax error at or near"),
            Signature(DbEngine.POSTGRES, "invalid input syntax for type"),

            // MSSQL
            Signature(DbEngine.MSSQL, "microsoft ole db provider for sql server"),
            Signature(DbEngine.MSSQL, "odbc sql server driver"),
            Signature(DbEngine.MSSQL, "sqlserver jdbc driver"),
            Signature(DbEngine.MSSQL, "incorrect syntax near"),
            Signature(DbEngine.MSSQL, "unclosed quotation mark"),
            Signature(DbEngine.MSSQL, "microsoft sql native client"),

            // Oracle
            Signature(DbEngine.ORACLE, "ora-00933"),
            Signature(DbEngine.ORACLE, "ora-00921"),
            Signature(DbEngine.ORACLE, "ora-01756"),
            Signature(DbEngine.ORACLE, "oracle error"),
            Signature(DbEngine.ORACLE, "quoted string not properly terminated"),

            // SQLite
            Signature(DbEngine.SQLITE, "sqlite3::query"),
            Signature(DbEngine.SQLITE, "sqlite error"),
            Signature(DbEngine.SQLITE, "sqlite3.operationalerror"),
            Signature(DbEngine.SQLITE, "unrecognized token:"),
            Signature(DbEngine.SQLITE, "near \"": syntax error")
        )

        val REGEX_SIGNATURES: List<RegexSignature> = listOf(
            RegexSignature(
                DbEngine.MYSQL,
                Pattern.compile("sql syntax.*?mysql", Pattern.CASE_INSENSITIVE)
            ),
            RegexSignature(
                DbEngine.POSTGRES,
                Pattern.compile("postgresql.*?error", Pattern.CASE_INSENSITIVE)
            ),
            RegexSignature(
                DbEngine.MSSQL,
                Pattern.compile("microsoft.*?sql.*?server", Pattern.CASE_INSENSITIVE)
            ),
            RegexSignature(
                DbEngine.ORACLE,
                Pattern.compile("ora-\\d{5}", Pattern.CASE_INSENSITIVE)
            ),
            RegexSignature(
                DbEngine.SQLITE,
                Pattern.compile("sqlite.*?error", Pattern.CASE_INSENSITIVE)
            )
        )

        fun excerptAround(body: String, needle: String): String {
            val lower = body.lowercase(Locale.ROOT)
            val idx = lower.indexOf(needle.lowercase(Locale.ROOT))
            if (idx < 0) {
                return body.take(Constants.RESPONSE_SNIPPET_MAX_LEN)
            }
            val radius = Constants.RESPONSE_SNIPPET_MAX_LEN / 2
            val start = (idx - radius).coerceAtLeast(0)
            val end = (idx + needle.length + radius).coerceAtMost(body.length)
            return body.substring(start, end)
        }
    }
}