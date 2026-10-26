package com.sf.tadami.ui.category

import androidx.compose.ui.state.ToggleableState
import com.sf.tadami.domain.category.Category

/** A row of the set categories dialog. [mixed]: only some of the anime are in the category */
data class CategoryCheckbox(
    val category: Category,
    val state: ToggleableState,
    val mixed: Boolean = false
)

/**
 * The rows of the dialog for anime in [animeCategories], the category ids of each: checked when all of them
 * are in the category, indeterminate when only some are. The Default category has no row.
 */
fun categoryCheckboxes(
    categories: List<Category>,
    animeCategories: Collection<Collection<Long>>
): List<CategoryCheckbox> {
    return categories.filterNot { it.isSystemCategory }.map { category ->
        val count = animeCategories.count { category.id in it }
        when {
            count == 0 -> CategoryCheckbox(category, ToggleableState.Off)
            count == animeCategories.size -> CategoryCheckbox(category, ToggleableState.On)
            else -> CategoryCheckbox(category, ToggleableState.Indeterminate, mixed = true)
        }
    }
}

/** A tap on the row: on and off, then back to indeterminate for a mixed one, which leaves it as it is */
fun CategoryCheckbox.next(): CategoryCheckbox {
    val nextState = when (state) {
        ToggleableState.Indeterminate -> ToggleableState.Off
        ToggleableState.Off -> ToggleableState.On
        ToggleableState.On -> if (mixed) ToggleableState.Indeterminate else ToggleableState.Off
    }
    return copy(state = nextState)
}

/** The categories to add the anime to, and to remove them from. An indeterminate row changes nothing */
fun List<CategoryCheckbox>.toChanges(): Pair<List<Long>, List<Long>> {
    val add = filter { it.state == ToggleableState.On }.map { it.category.id }
    val remove = filter { it.state == ToggleableState.Off }.map { it.category.id }
    return add to remove
}
