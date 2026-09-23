package com.yourapp.sqliautohunter.domain.model

data class DorkTemplate(
    val name: String,
    val template: String,
    val description: String = ""
) {
    companion object {
        val DEFAULT_TEMPLATES = listOf(
            DorkTemplate(
                name = "PHP ID Parameter",
                template = "inurl:php?id=",
                description = "Target PHP pages with id parameter"
            ),
            DorkTemplate(
                name = "Index PHP ID",
                template = "inurl:index.php?id=",
                description = "Target index.php pages with id parameter"
            ),
            DorkTemplate(
                name = "Product PHP Category",
                template = "inurl:product.php?catid=",
                description = "Target product pages with category id"
            ),
            DorkTemplate(
                name = "News PHP ID",
                template = "inurl:news.php?id=",
                description = "Target news pages with id parameter"
            ),
            DorkTemplate(
                name = "Page PHP ID",
                template = "inurl:page.php?id=",
                description = "Target generic pages with id parameter"
            ),
            DorkTemplate(
                name = "Item PHP ID",
                template = "inurl:item.php?id=",
                description = "Target item pages with id parameter"
            ),
            DorkTemplate(
                name = "View PHP ID",
                template = "inurl:view.php?id=",
                description = "Target view pages with id parameter"
            ),
            DorkTemplate(
                name = "Details PHP ID",
                template = "inurl:details.php?id=",
                description = "Target details pages with id parameter"
            ),
            DorkTemplate(
                name = "Category PHP ID",
                template = "inurl:category.php?id=",
                description = "Target category pages with id parameter"
            ),
            DorkTemplate(
                name = "Search PHP Query",
                template = "inurl:search.php?q=",
                description = "Target search pages with query parameter"
            )
        )
    }
}
