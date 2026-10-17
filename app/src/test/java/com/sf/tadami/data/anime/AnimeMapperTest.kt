package com.sf.tadami.data.anime

import com.sf.tadami.domain.category.Category
import org.junit.Assert.assertEquals
import org.junit.Test

class AnimeMapperTest {

    @Test
    fun theLibraryViewCategoriesAreParsed() {
        assertEquals(listOf(3L, 1L), AnimeMapper.parseCategories("3,1"))
    }

    @Test
    fun anAnimeInNoCategoryIsInTheDefaultOne() {
        assertEquals(listOf(Category.UNCATEGORIZED_ID), AnimeMapper.parseCategories("0"))
        assertEquals(listOf(Category.UNCATEGORIZED_ID), AnimeMapper.parseCategories(""))
    }
}
