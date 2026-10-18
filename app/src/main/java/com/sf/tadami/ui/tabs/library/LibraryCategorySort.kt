package com.sf.tadami.ui.tabs.library

import com.sf.tadami.domain.category.Category
import com.sf.tadami.preferences.library.LibrarySort
import com.sf.tadami.preferences.library.LibrarySort.Companion.SORT_DIRECTION
import com.sf.tadami.preferences.library.LibrarySort.Companion.SORT_DIRECTION_MASK
import com.sf.tadami.preferences.library.LibrarySort.Companion.SORT_TYPE_MASK
import com.sf.tadami.ui.tabs.library.bottomsheet.setFlags

/**
 * The sort of a category's tab. With the per category sort, its own one: in its flags, in [defaultCategoryFlags]
 * for the Default category, which isn't stored. A category without one yet (flags 0), and every category
 * without the per category sort, use the library sort.
 */
internal fun categorySort(
    category: Category,
    perCategory: Boolean,
    librarySort: LibrarySort,
    defaultCategoryFlags: Long,
): LibrarySort {
    if (!perCategory) return librarySort
    val flags = if (category.isSystemCategory) defaultCategoryFlags else category.flags
    return if (flags == 0L) librarySort else LibrarySort(flags)
}

/** The sort after a tap on the [type] row: the other direction for the current type, else that type */
internal fun LibrarySort.select(type: Long): LibrarySort {
    val newFlags = if (type == sortType.flag) {
        flags.setFlags(if (isAscending) 0L else SORT_DIRECTION, SORT_DIRECTION_MASK)
    } else {
        flags.setFlags(type, SORT_TYPE_MASK)
    }
    return LibrarySort(newFlags)
}
