package com.sf.tadami.data.interactors.category

import com.sf.tadami.data.category.CategoryRepository

class SetAnimeCategories(
    private val categoryRepository: CategoryRepository,
) {

    /** The anime ends up in [categoryIds] only, in the Default category when empty */
    suspend fun await(animeId: Long, categoryIds: Collection<Long>) {
        categoryRepository.setAnimeCategories(animeId, categoryIds)
    }

    /** Adds the anime to [add] and removes them from [remove], in one go: their other categories stay */
    suspend fun await(animeIds: Collection<Long>, add: Collection<Long>, remove: Collection<Long>) {
        categoryRepository.updateAnimeCategories(animeIds, add, remove)
    }
}
