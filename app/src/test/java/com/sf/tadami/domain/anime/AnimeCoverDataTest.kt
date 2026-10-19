package com.sf.tadami.domain.anime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AnimeCoverDataTest {

    @Test
    fun aCoverNeverRefreshedKeepsItsURLAsKey() {
        // The key the covers had before, so the image caches stay
        assertEquals("https://cdn/cover.jpg", AnimeCoverData("https://cdn/cover.jpg", 0L).cacheKey)
    }

    @Test
    fun aRefreshedCoverGetsANewKeyAtTheSameURL() {
        val before = AnimeCoverData("https://cdn/cover.jpg", 1L).cacheKey
        val after = AnimeCoverData("https://cdn/cover.jpg", 2L).cacheKey

        assertEquals("https://cdn/cover.jpg;2", after)
        assertNotEquals(before, after)
    }

    @Test
    fun noURLNoCover() {
        assertNull(AnimeCoverData.of(null, 1L))
        assertNull(AnimeCoverData.of("", 1L))
        assertEquals(AnimeCoverData("https://cdn/cover.jpg", 3L), AnimeCoverData.of("https://cdn/cover.jpg", 3L))
    }

    @Test
    fun anAnimeCoverCarriesWhenItWasRefreshed() {
        val anime = Anime.create().copy(thumbnailUrl = "https://cdn/cover.jpg", coverLastModified = 5L)

        assertEquals(AnimeCoverData("https://cdn/cover.jpg", 5L), anime.coverData)
    }
}
