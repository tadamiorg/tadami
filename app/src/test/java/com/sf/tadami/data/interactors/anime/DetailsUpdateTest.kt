package com.sf.tadami.data.interactors.anime

import com.sf.tadami.domain.anime.UpdateAnime
import com.sf.tadami.source.model.SAnime
import com.sf.tadami.source.model.SAnimeStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class DetailsUpdateTest {

    @Test
    fun theFetchedDetailsAreStored() {
        val remote = SAnime.create().apply {
            url = "/anime"
            title = "Title"
            rawTitle = "Raw title"
            language = "VF"
            thumbnailUrl = "https://cdn/cover.jpg"
            release = "2024"
            studio = "Studio"
            author = "Author"
            status = SAnimeStatus.ONGOING
            description = "Description"
            genres = listOf("Action")
        }

        assertEquals(
            UpdateAnime(
                id = 1L,
                title = "Title",
                rawTitle = "Raw title",
                language = "VF",
                thumbnailUrl = "https://cdn/cover.jpg",
                release = "2024",
                studio = "Studio",
                author = "Author",
                status = SAnimeStatus.ONGOING,
                description = "Description",
                genres = listOf("Action"),
                initialized = true,
            ),
            remote.toDetailsUpdate(1L),
        )
    }

    @Test
    fun whatTheSourceDoesntGiveKeepsTheStoredValue() {
        // No title, an empty cover and an unknown status: null in the update, which keeps the stored value
        val remote = SAnime.create().apply {
            url = "/anime"
            thumbnailUrl = ""
        }

        assertEquals(UpdateAnime(id = 1L, initialized = true), remote.toDetailsUpdate(1L))
    }

    @Test
    fun theLibraryStateAndTheUpdateDatesAreKept() {
        val remote = SAnime.create().apply {
            url = "/other"
            title = "Title"
            status = SAnimeStatus.COMPLETED
        }

        val update = remote.toDetailsUpdate(1L)

        // Its source and URL stay, as the user's library state and the episodes update dates
        assertEquals(null, update.source)
        assertEquals(null, update.url)
        assertEquals(null, update.favorite)
        assertEquals(null, update.dateAdded)
        assertEquals(null, update.episodeFlags)
        assertEquals(null, update.lastUpdate)
        assertEquals(null, update.nextUpdate)
        assertEquals(null, update.fetchInterval)
    }
}
