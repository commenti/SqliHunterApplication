package com.yourapp.sqliautohunter.domain.model

/**
 * A single search-engine dork pattern.
 *
 * `pattern` contains a `{kw}` placeholder that gets substituted with the user's
 * keyword at query-generation time. `category` groups templates so the UI can
 * offer "all", "php", "generic", etc. `requiresExactParam` flags templates that
 * only make sense when the keyword itself is a param name (e.g. inurl:?p=).
 */
data class DorkTemplate(
    val id: String,
    val pattern: String,
    val category: DorkCategory,
    val requiresExactParam: Boolean = false
) {
    /** Replace {kw} with the given keyword and return the concrete dork string. */
    fun render(keyword: String): String =
        pattern.replace("{kw}", keyword.trim())

    companion object {
        /**
         * Default catalogue — mirrors Constants.DORK_TEMPLATES but as typed
         * objects so the UI can group and filter.
         */
        val DEFAULT: List<DorkTemplate> = listOf(
            DorkTemplate("php_id",       "inurl:php?id={kw}",          DorkCategory.PHP),
            DorkTemplate("index_php_id", "inurl:index.php?id={kw}",    DorkCategory.PHP),
            DorkTemplate("product_php",  "inurl:product.php?id={kw}",  DorkCategory.PHP),
            DorkTemplate("news_php",     "inurl:news.php?id={kw}",     DorkCategory.PHP),
            DorkTemplate("article_php",  "inurl:article.php?id={kw}",  DorkCategory.PHP),
            DorkTemplate("item_php",     "inurl:item.php?id={kw}",     DorkCategory.PHP),
            DorkTemplate("view_php",     "inurl:view.php?id={kw}",     DorkCategory.PHP),
            DorkTemplate("page_php",     "inurl:page.php?id={kw}",     DorkCategory.PHP),
            DorkTemplate("display_php",  "inurl:display.php?id={kw}",  DorkCategory.PHP),
            DorkTemplate("detail_php",   "inurl:detail.php?id={kw}",   DorkCategory.PHP),

            DorkTemplate("catid",        "inurl:catid={kw}",           DorkCategory.CATEGORY),
            DorkTemplate("cat",          "inurl:?cat={kw}&id=",        DorkCategory.CATEGORY),

            DorkTemplate("site_com",     "site:.com inurl:catid={kw}", DorkCategory.TLD_SCOPED),
            DorkTemplate("site_net",     "site:.net inurl:product_id={kw}", DorkCategory.TLD_SCOPED),
            DorkTemplate("site_org",     "site:.org inurl:item_id={kw}",    DorkCategory.TLD_SCOPED),

            DorkTemplate("generic_p",    "inurl:?p={kw}",              DorkCategory.GENERIC),
            DorkTemplate("generic_page", "inurl:?page={kw}",           DorkCategory.GENERIC),
            DorkTemplate("generic_view", "inurl:?view={kw}",           DorkCategory.GENERIC),
            DorkTemplate("generic_prod", "inurl:?product={kw}",        DorkCategory.GENERIC)
        )

        fun byId(id: String): DorkTemplate? = DEFAULT.firstOrNull { it.id == id }

        fun byCategory(category: DorkCategory): List<DorkTemplate> =
            DEFAULT.filter { it.category == category }
    }
}

/** Coarse grouping used by the template picker UI. */
enum class DorkCategory(val label: String) {
    PHP("PHP"),
    CATEGORY("Category"),
    TLD_SCOPED("TLD-scoped"),
    GENERIC("Generic");

    companion object {
        fun all(): List<DorkCategory> = entries.toList()
    }
}