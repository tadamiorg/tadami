package com.sf.tadami.ui.tabs.library

import com.sf.tadami.domain.category.Category
import com.sf.tadami.ui.components.data.LibraryItem

/** A tab of the library: a category and its anime */
data class LibraryTab(
    val category: Category,
    val items: List<LibraryItem>
)

/**
 * One tab per category, by order. The Default category, of the anime in none, comes first when it has
 * some or when there's no category at all. An anime in several categories is in each of their tabs.
 */
internal fun libraryTabs(categories: List<Category>, library: List<LibraryItem>): List<LibraryTab> {
    val itemsByCategory = HashMap<Long, MutableList<LibraryItem>>()
    library.forEach { item ->
        item.anime.categories.forEach { categoryId ->
            itemsByCategory.getOrPut(categoryId) { mutableListOf() }.add(item)
        }
    }
    val userCategories = categories.filterNot { it.isSystemCategory }
    val defaultItems = itemsByCategory[Category.UNCATEGORIZED_ID].orEmpty()
    val defaultTab = LibraryTab(Category.default(), defaultItems)
        .takeIf { defaultItems.isNotEmpty() || userCategories.isEmpty() }
    return listOfNotNull(defaultTab) + userCategories.map { LibraryTab(it, itemsByCategory[it.id].orEmpty()) }
}

/** The tab row is only worth showing with more than one tab */
internal fun List<LibraryTab>.showTabRow(): Boolean = size > 1

/** The item count of a tab, shown with the number of items setting on or while searching */
internal fun LibraryTab.badgeCount(showNumberOfItems: Boolean, searching: Boolean): Int? {
    return items.size.takeIf { showNumberOfItems || searching }
}
