package com.sf.tadami.notifications.libraryupdate

import com.sf.tadami.domain.anime.LibraryAnime

/**
 * The anime the library update goes through: the ones in an [include] category, all of them when there's
 * none, but not the ones in an [exclude] category, which wins. The category ids as the settings store them,
 * the Default category's one included.
 */
/** The anime of [categoryId] when the update is for one category, whatever the settings, else the settings' ones */
internal fun List<LibraryAnime>.toUpdate(categoryId: Long?, include: Set<String>, exclude: Set<String>): List<LibraryAnime> {
    return if (categoryId != null) filter { categoryId in it.categories } else inUpdateCategories(include, exclude)
}

internal fun List<LibraryAnime>.inUpdateCategories(include: Set<String>, exclude: Set<String>): List<LibraryAnime> {
    val included = include.mapNotNullTo(HashSet()) { it.toLongOrNull() }
    val excluded = exclude.mapNotNullTo(HashSet()) { it.toLongOrNull() }
    return filter { anime ->
        (included.isEmpty() || anime.categories.any { it in included }) && anime.categories.none { it in excluded }
    }
}
