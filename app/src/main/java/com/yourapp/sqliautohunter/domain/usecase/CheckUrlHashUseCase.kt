package com.yourapp.sqliautohunter.domain.usecase

import com.yourapp.sqliautohunter.data.local.database.dao.TestedUrlHashDao
import com.yourapp.sqliautohunter.data.local.database.entity.TestedUrlHashEntity
import com.yourapp.sqliautohunter.domain.model.ScanTarget
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Deduplication gate. Every URL that reaches the queue passes through here.
 *
 * The single source of truth is `tested_urls_hash`, keyed by SHA-256 of the
 * normalized URL. The DAO's `checkAndInsert` is transactional and uses
 * OnConflictStrategy.IGNORE on the primary key — so concurrent workers racing
 * on the same normalized URL resolve deterministically: exactly one wins the
 * insert, everyone else gets back the existing row.
 *
 * Outcomes:
 *   Fresh    -> this call won the insert. The URL is new and must be queued.
 *   Known    -> the URL was already tested. Surface the cached result; do not
 *               re-queue unless [allowRetest] is set and the prior result was
 *               an error.
 *   Rejected -> caller passed a target with null hash. Nothing was inserted.
 *
 * The use case never touches the queue — queue insertion is QueueRepository's
 * job. This keeps the gate pure: check + record, one decision, one answer.
 */
@Singleton
class CheckUrlHashUseCase @Inject constructor(
    private val dao: TestedUrlHashDao
) {

    sealed interface Outcome {
        /** URL was not in the ledger — inserted, ready to queue. */
        data class Fresh(val target: ScanTarget, val hash: String) : Outcome

        /** URL already exists — returned the cached row. */
        data class Known(
            val target: ScanTarget,
            val hash: String,
            val cached: TestedUrlHashEntity
        ) : Outcome

        /** Target had no hash — normalization was skipped or failed. */
        data class Rejected(val target: ScanTarget, val reason: String) : Outcome
    }

    data class Options(
        /**
         * When true, a prior `error` result is allowed through as Fresh —
         * caller wants to retry previously-failed URLs without clearing the
         * ledger. Successful results are never retested by this flag.
         */
        val allowRetest: Boolean = false
    )

    /**
     * Check the ledger and record the URL if new.
     *
     * @param target a ScanTarget with a non-null `urlHash`. Use
     *               NormalizeUrlUseCase first — this gate does not normalize.
     */
    suspend operator fun invoke(
        target: ScanTarget,
        options: Options = Options()
    ): Outcome {
        val hash = target.urlHash
        val normalized = target.normalizedUrl

        if (hash.isNullOrEmpty() || normalized.isNullOrEmpty()) {
            return Outcome.Rejected(target, "target not normalized")
        }

        val placeholder = TestedUrlHashEntity(
            urlHash = hash,
            originalUrl = normalized,
            testResult = TestedUrlHashEntity.RESULT_ERROR,
            testedAt = System.currentTimeMillis(),
            payloadTypesTried = "[]"
        )

        val check = dao.checkAndInsert(placeholder)

        if (check.isNew) {
            return Outcome.Fresh(target, hash)
        }

        val existing = check.existing
        if (existing != null &&
            options.allowRetest &&
            existing.testResult == TestedUrlHashEntity.RESULT_ERROR
        ) {
            // Retest path: caller wants another shot at a URL that previously
            // errored. We surface it as Fresh without touching the ledger row —
            // the row's test_result will be overwritten on finalize via
            // markResult() once the retest completes.
            return Outcome.Fresh(target, hash)
        }

        return if (existing != null) {
            Outcome.Known(target, hash, existing)
        } else {
            // Extremely narrow race: row existed at check time but was pruned
            // before we could read it back. Treat as Fresh so the URL isn't
            // silently dropped.
            Outcome.Fresh(target, hash)
        }
    }

    /**
     * Batch check. Preserves input order in the `fresh` output so queue
     * insertion stays deterministic. Returns both buckets — the caller decides
     * whether Known rows need surfacing or are silently skipped.
     */
    suspend fun batch(
        targets: List<ScanTarget>,
        options: Options = Options()
    ): BatchOutcome {
        val fresh = ArrayList<ScanTarget>(targets.size)
        val known = ArrayList<Outcome.Known>(targets.size)
        val rejected = ArrayList<Outcome.Rejected>()

        for (t in targets) {
            when (val out = invoke(t, options)) {
                is Outcome.Fresh -> fresh += out.target
                is Outcome.Known -> known += out
                is Outcome.Rejected -> rejected += out
            }
        }

        return BatchOutcome(fresh, known, rejected)
    }

    data class BatchOutcome(
        val fresh: List<ScanTarget>,
        val known: List<Outcome.Known>,
        val rejected: List<Outcome.Rejected>
    )

    /**
     * Finalize a URL's ledger row