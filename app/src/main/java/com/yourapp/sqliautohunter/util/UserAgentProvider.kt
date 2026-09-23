package com.yourapp.sqliautohunter.util

import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

/**
 * Rotating User-Agent bundles.
 *
 * A "bundle" is a matched set: UA string + sec-ch-ua + sec-ch-ua-mobile +
 * sec-ch-ua-platform. Mixing a Chrome 125 UA with a Chrome 118 sec-ch-ua
 * header is a fingerprinting tell — the bundles in Constants.UA_BUNDLES are
 * kept internally consistent, and this provider never splits them.
 *
 * Two rotation strategies:
 *   - nextRoundRobin() — deterministic, evenly spreads over the pool. Use for
 *     retries on the same target so the same UA isn't hammered twice in a row.
 *   - random()         — uniform pick. Use for fresh search queries.
 */
object UserAgentProvider {

    private val counter = AtomicInteger(0)

    /** Deterministic rotation. Thread-safe. */
    fun nextRoundRobin(): Constants.UaBundle {
        val idx = counter.getAndIncrement()
        val pool = Constants.UA_BUNDLES
        return pool[Math.floorMod(idx, pool.size)]
    }

    /** Random pick — no immediate-repeat avoidance, caller's job if needed. */
    fun random(): Constants.UaBundle {
        val pool = Constants.UA_BUNDLES
        return pool[Random.nextInt(pool.size)]
    }

    /**
     * Random pick that never returns [avoid] on this call. Falls back to any
     * entry if the pool has only one element.
     */
    fun randomAvoiding(avoid: Constants.UaBundle?): Constants.UaBundle {
        val pool = Constants.UA_BUNDLES
        if (pool.size <= 1 || avoid == null) return random()
        var pick = random()
        var guard = 0
        while (pick == avoid && guard < 4) {
            pick = random()
            guard++
        }
        return pick
    }

    /**
     * Apply a bundle's headers to an OkHttp Request.Builder.
     * Caller owns the builder lifecycle — this only mutates headers.
     */
    fun applyTo(builder: okhttp3.Request.Builder, bundle: Constants.UaBundle): okhttp3.Request.Builder {
        builder.header("User-Agent", bundle.userAgent)
        bundle.secChUa?.let { builder.header("sec-ch-ua", it) }
        bundle.secChUaMobile?.let { builder.header("sec-ch-ua-mobile", it) }
        bundle.secChUaPlatform?.let { builder.header("sec-ch-ua-platform", it) }
        builder.header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
        builder.header("Accept-Language", "en-US,en;q=0.9")
        builder.header("Cache-Control", "no-cache")
        builder.header("Pragma", "no-cache")
        return builder
    }

    /** Same as applyTo but for a WebView's settings via an extra-headers map. */
    fun toHeaderMap(bundle: Constants.UaBundle): Map<String, String> {
        val out = LinkedHashMap<String, String>(8)
        out["User-Agent"] = bundle.userAgent
        bundle.secChUa?.let { out["sec-ch-ua"] = it }
        bundle.secChUaMobile?.let { out["sec-ch-ua-mobile"] = it }
        bundle.secChUaPlatform?.let { out["sec-ch-ua-platform"] = it }
        return out
    }
}