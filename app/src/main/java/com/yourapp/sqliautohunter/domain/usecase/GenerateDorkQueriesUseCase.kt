package com.yourapp.sqliautohunter.domain.usecase

import com.yourapp.sqliautohunter.domain.model.DorkCategory
import com.yourapp.sqliautohunter.domain.model.DorkTemplate
import javax.inject.Inject

/**
 * Turn a list of user keywords into a list of concrete dork query strings.
 *
 * Contract:
 *   - One dork per (keyword, template) pair.
 *   - Templates whose `requiresExactParam` is set are only included when the
 *     keyword looks like a parameter name (matches /^[a-z_][a-z0-9_]*$/i).
 *   - Duplicates after case-folding are collapsed.
 *   - Order is deterministic: keyword outer loop, template order inner.
 *
 * Pure function — no IO, no state. Safe to call from any thread.
 */
class GenerateDorkQueriesUseCase @Inject constructor() {

    data class Request(
        val keywords: List<String>,
        val templateIds: List<String>? = null,
        val categories: Set<DorkCategory>? = null,
        val maxPerKeyword: Int = Int.MAX_VALUE
    )

    data class Result(
        val queries: List<String>,
        val byKeyword: Map<String, List<String>>
    )

    operator fun invoke(request: Request): Result {
        val keywords = request.keywords
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .toList()

        if (keywords.isEmpty()) return Result(emptyList(), emptyMap())

        val templates = selectTemplates(request)
        if (templates.isEmpty()) return Result(emptyList(), emptyMap())

        val out = LinkedHashMap<String, MutableList<String>>(keywords.size)
        val seen = HashSet<String>(keywords.size * templates.size)

        for (kw in keywords) {
            val bucket = ArrayList<String>(templates.size)
            val isParamLike = PARAM_NAME_REGEX.matches(kw)

            for (t in templates) {
                if (t.requiresExactParam && !isParamLike) continue
                val rendered = t.render(kw)
                val key = rendered.lowercase()
                if (!seen.add(key)) continue
                bucket += rendered
                if (bucket.size >= request.maxPerKeyword) break
            }

            if (bucket.isNotEmpty()) out[kw] = bucket
        }

        val flat = out.values.flatten()
        return Result(queries = flat, byKeyword = out)
    }

    private fun selectTemplates(request: Request): List<DorkTemplate> {
        val all = DorkTemplate.DEFAULT
        var filtered: List<DorkTemplate> = all

        request.categories?.let { cats ->
            if (cats.isNotEmpty()) {
                filtered = filtered.filter { it.category in cats }
            }
        }

        request.templateIds?.let { ids ->
            if (ids.isNotEmpty()) {
                val idSet = ids.toHashSet()
                filtered = filtered.filter { it.id in idSet }
            }
        }

        return filtered
    }

    private companion object {
        val PARAM_NAME_REGEX = Regex("^[a-z_][a-z0-9_]*$", RegexOption.IGNORE_CASE)
    }
}