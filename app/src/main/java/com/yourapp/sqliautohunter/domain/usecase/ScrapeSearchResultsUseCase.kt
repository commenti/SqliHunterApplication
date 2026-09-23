package com.yourapp.sqliautohunter.domain.usecase

import com.yourapp.sqliautohunter.data.remote.search.SearchEngineClient
import com.yourapp.sqliautohunter.data.remote.search.SearchHit
import com.yourapp.sqliautohunter.data.remote.search.SearchSource
import com.yourapp.sqliautohunter.util.Constants
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Orchestrates multi-engine search scraping for a batch of dork queries.
 *
 * Contract:
 *   - For each query, tries engines in [SearchSource.priorityOrder] until one
 *     returns a non-empty page or all are exhausted.
 *   - Per-engine circuit breaker: after N consecutive failures an engine is
 *     parked for a cooldown window. Breaker state is in-memory (this use case
 *     owns it) — survives config changes because the use case is @Singleton.
 *   - Emits results as a cold Flow so the caller can stream to the queue.
 *   - Dedup is NOT performed here — the pipeline hashes and dedups downstream
 *     in CheckUrlHashUseCase. This use case emits raw hits.
 *
 * The use case is @Singleton and thread-safe: `breakers` is guarded by a
 * mutex; per-query fan-out uses structured concurrency.
 */
class ScrapeSearchResultsUseCase @Inject constructor(
    private val client: SearchEngineClient
) {

    data class Request(
        val queries: List<String>,
        val keywordSource: String,
        val pagesPerQuery: Int = Constants.SEARCH_MAX_PAGES_PER_KEYWORD,
        val engines: List<SearchSource> = SearchSource.priorityOrder()
    )

    data class Batch(
        val keywordSource: String,
        val hits: List<SearchHit>,
        val enginesUsed: Set<SearchSource>,
        val enginesSkipped: Set<SearchSource>
    )

    /**
     * Stream hits for each query. Backpressure is natural — the consumer pulls.
     * Cancellation-safe: killing the collector cancels in-flight scrapes.
     */
    fun stream(request: Request): Flow<Batch> = flow {
        if (request.queries.isEmpty() || request.engines.isEmpty()) return@flow

        for (query in request.queries) {
            val batch = fetchForQuery(query, request)
            if (batch.hits.isNotEmpty()) emit(batch)
        }
    }

    /**
     * One-shot variant — collects all batches and returns them. Use from tests
     * or the manual-URL path where the whole list is needed up front.
     */
    suspend fun scrapeAll(request: Request): List<Batch> {
        val out = ArrayList<Batch>(request.queries.size)
        stream(request).collect { out += it }
        return out
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private suspend fun fetchForQuery(query: String, request: Request): Batch =
        coroutineScope {
            val enginesUsed = LinkedHashSet<SearchSource>()
            val enginesSkipped = LinkedHashSet<SearchSource>()
            val hits = ArrayList<SearchHit>(Constants.SEARCH_RESULTS_PER_PAGE)

            for (engine in request.engines) {
                if (!tryAcquireBreaker(engine)) {
                    enginesSkipped += engine
                    continue
                }

                val pages = try {
                    (0 until request.pagesPerQuery).map { page ->
                        async { runScrape(engine, query, page) }
                    }.awaitAll()
                } catch (ce: CancellationException) {
                    throw ce
                } catch (_: Throwable) {
                    recordFailure(engine)
                    enginesSkipped += engine
                    continue
                }

                val pageHits = pages.flatten()
                if (pageHits.isEmpty()) {
                    recordFailure(engine)
                    enginesSkipped += engine
                    continue
                }

                recordSuccess(engine)
                enginesUsed += engine
                hits += pageHits

                // First non-empty engine wins for this query — engines are
                // ordered by priority, so we don't hammer the fallbacks.
                break
            }

            Batch(
                keywordSource = request.keywordSource,
                hits = hits.distinctBy { it.url },
                enginesUsed = enginesUsed,
                enginesSkipped = enginesSkipped
            )
        }

    private suspend fun runScrape(
        engine: SearchSource,
        query: String,
        page: Int
    ): List<SearchHit> {
        // Retry-with-backoff lives in the client (it owns OkHttp + status codes);
        // this use case only routes and records breaker state. The delay here
        // spreads queries across engines so we don't burst a single host.
        if (page > 0) delay(Constants.DEFAULT_SCAN_DELAY_MS * page)
        return try {
            client.search(engine, query, page)
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: Throwable) {
            emptyList()
        }
    }

    // ------------------------------------------------------------------
    // Circuit breaker
    // ------------------------------------------------------------------

    private data class BreakerState(
        var consecutiveFailures: Int = 0,
        var parkedUntil: Long = 0L
    )

    private val breakers = HashMap<SearchSource, BreakerState>(SearchSource.entries.size)
    private val breakerLock = Any()

    private fun tryAcquireBreaker(engine: SearchSource): Boolean = synchronized(breakerLock) {
        val state = breakers.getOrPut(engine) { BreakerState() }
        val now = System.currentTimeMillis()
        if (state.parkedUntil > now) return false
        if (state.parkedUntil != 0L && state.parkedUntil <= now) {
            // Cooldown elapsed — reset and let it back in.
            state.parkedUntil = 0L
            state.consecutiveFailures = 0
        }
        true
    }

    private fun recordFailure(engine: SearchSource) = synchronized(breakerLock) {
        val state = breakers.getOrPut(engine) { BreakerState() }
        state.consecutiveFailures++
        if (state.consecutiveFailures >= Constants.SEARCH_CIRCUIT_BREAKER_THRESHOLD) {
            state.parkedUntil = System.currentTimeMillis() + Constants.SEARCH_CIRCUIT_COOLDOWN_MS
            state.consecutiveFailures = 0
        }
    }

    private fun recordSuccess(engine: SearchSource) = synchronized(breakerLock) {
        val state = breakers.getOrPut(engine) { BreakerState() }
        state.consecutiveFailures = 0
        state.parkedUntil = 0L
    }

    /** Snapshot for diagnostics — never mutate the returned map. */
    fun breakerSnapshot(): Map<SearchSource, Pair<Int, Long>> = synchronized(breakerLock) {
        breakers.mapValues { (_, s) -> s.consecutiveFailures to s.parkedUntil }
    }
}