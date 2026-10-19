package com.sf.tadami.data.interactors.anime

import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.domain.anime.UpdateAnime
import com.sf.tadami.source.model.SAnime
import com.sf.tadami.source.model.SAnimeStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class DetailsUpdateTest {

    private val local = Anime.create().copy(id = 1L, url = "/anime", title = "Stored", thumbnailUrl = "https://cdn/cover.jpg")

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
            remote.toDetailsUpdate(local),
        )
    }

    @Test
    fun whatTheSourceDoesntGiveKeepsTheStoredValue() {
        // No title, an empty cover and an unknown status: null in the update, which keeps the stored value
        val remote = SAnime.create().apply {
            url = "/anime"
            thumbnailUrl = ""
        }

        assertEquals(UpdateAnime(id = 1L, initialized = true), remote.toDetailsUpdate(local))
    }

    @Test
    fun theLibraryStateAndTheUpdateDatesAreKept() {
        val remote = SAnime.create().apply {
            url = "/other"
            title = "Title"
            status = SAnimeStatus.COMPLETED
        }

        val update = remote.toDetailsUpdate(local)

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

    @Test
    fun aNewCoverURLMarksTheCoverAsModified() {
        val remote = SAnime.create().apply {
            url = "/anime"
            thumbnailUrl = "https://cdn/new.jpg"
        }

        assertEquals(42L, remote.toDetailsUpdate(local, now = 42L).coverLastModified)
    }

    @Test
    fun theSameCoverURLKeepsTheCoverUnlessItsRefreshed() {
        val remote = SAnime.create().apply {
            url = "/anime"
            thumbnailUrl = "https://cdn/cover.jpg"
        }

        assertEquals(null, remote.toDetailsUpdate(local, now = 42L).coverLastModified)
        // The library's details refresh downloads it again: same URL, new image cache key
        assertEquals(42L, remote.toDetailsUpdate(local, refreshCover = true, now = 42L).coverLastModified)
    }

    @Test
    fun noCoverFromTheSourceKeepsTheStoredOne() {
        val remote = SAnime.create().apply { url = "/anime" }

        val update = remote.toDetailsUpdate(local, now = 42L)

        assertEquals(null, update.thumbnailUrl)
        assertEquals(null, update.coverLastModified)
    }
}
