package com.yourapp.sqliautohunter.domain.usecase

import com.yourapp.sqliautohunter.domain.model.DorkTemplate
import com.yourapp.sqliautohunter.util.Constants

class GenerateDorkQueriesUseCase {

    operator fun invoke(
        keywords: List<String>,
        templates: List<DorkTemplate> = DorkTemplate.DEFAULT_TEMPLATES,
        searchEngines: List<String> = listOf("site:")
    ): List<String> {
        val queries = mutableListOf<String>()
        
        keywords.forEach { keyword ->
            templates.forEach { template ->
                val query = buildQuery(keyword, template.template, searchEngines)
                queries.add(query)
            }
        }
        
        return queries.distinct()
    }

    private fun buildQuery(
        keyword: String,
        template: String,
        searchEngines: List<String>
    ): String {
        val cleanKeyword = keyword.trim()
        
        // If template already contains the keyword or is a site search
        if (template.contains("site:") && !cleanKeyword.startsWith("http")) {
            return "$template$cleanKeyword $template"
        }
        
        // If template has a parameter
        if (template.contains("=")) {
            return "$template$cleanKeyword"
        }
        
        // Default: combine with space
        return "$cleanKeyword $template"
    }

    fun generateSiteSpecificQueries(
        keyword: String,
        site: String,
        templates: List<String> = Constants.DORK_TEMPLATES
    ): List<String> {
        return templates.map { template ->
            val baseQuery = if (template.contains("site:")) {
                template.replace("site:", "site:$site")
            } else {
                "site:$site $template"
            }
            "$baseQuery$keyword"
        }.distinct()
    }

    fun generateQueriesForUrl(url: String): List<String> {
        val domain = extractDomain(url)
        return generateSiteSpecificQueries("", domain)
    }

    private fun extractDomain(url: String): String {
        return when {
            url.startsWith("http://") -> url.substring(7).substringBefore("/")
            url.startsWith("https://") -> url.substring(8).substringBefore("/")
            else -> url.substringBefore("/")
        }
    }

    fun generateBatchQueries(
        keywords: List<String>,
        batchSize: Int = 10
    ): List<List<String>> {
        val allQueries = invoke(keywords)
        return allQueries.chunked(batchSize)
    }
}
