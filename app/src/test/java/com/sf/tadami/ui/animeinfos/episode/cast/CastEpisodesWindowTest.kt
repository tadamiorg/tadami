package com.sf.tadami.ui.animeinfos.episode.cast

import com.sf.tadami.domain.episode.Episode
import org.junit.Assert.assertEquals
import org.junit.Test

class CastEpisodesWindowTest {

    private val episodes = (1L..1000L).map { Episode.create().copy(id = it) }

    private fun ids(currentId: Long, radius: Int = 2) = castEpisodesWindow(episodes, currentId, radius).map { it.id }

    @Test
    fun theWindowKeepsTheNeighboursOfTheCurrentEpisode() {
        assertEquals(listOf(498L, 499L, 500L, 501L, 502L), ids(500L))
    }

    @Test
    fun theWindowStopsAtTheEndsOfTheList() {
        assertEquals(listOf(1L, 2L, 3L), ids(1L))
        assertEquals(listOf(998L, 999L, 1000L), ids(1000L))
    }

    @Test
    fun aShortListIsSentWhole() {
        assertEquals(episodes.take(5), castEpisodesWindow(episodes.take(5), 3L))
    }

    @Test
    fun anUnknownCurrentEpisodeSendsTheStartOfTheList() {
        assertEquals(listOf(1L, 2L, 3L), ids(-5L))
    }
}
