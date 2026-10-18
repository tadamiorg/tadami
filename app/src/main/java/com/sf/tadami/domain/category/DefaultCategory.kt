package com.sf.tadami.domain.category

/** Where an anime added to the library goes, from the default category setting */
sealed interface DefaultCategoryChoice {
    /** Asked with the set categories dialog */
    data object Ask : DefaultCategoryChoice

    /** In these categories, none for the Default category */
    data class Categories(val ids: List<Long>) : DefaultCategoryChoice
}

/**
 * The setting's category when it still exists, else the Default category when it is the one chosen or when
 * there's no category to choose from. Else the user is asked, the setting's default.
 */
fun defaultCategoryChoice(defaultCategory: Int, categories: List<Category>): DefaultCategoryChoice {
    val userCategories = categories.filterNot { it.isSystemCategory }
    val category = userCategories.find { it.id == defaultCategory.toLong() }
    return when {
        category != null -> DefaultCategoryChoice.Categories(listOf(category.id))
        defaultCategory.toLong() == Category.UNCATEGORIZED_ID || userCategories.isEmpty() -> {
            DefaultCategoryChoice.Categories(emptyList())
        }
        else -> DefaultCategoryChoice.Ask
    }
}
