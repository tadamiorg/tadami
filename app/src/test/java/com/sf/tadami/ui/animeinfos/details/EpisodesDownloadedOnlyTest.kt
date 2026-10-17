package com.sf.tadami.ui.animeinfos.details

import com.sf.tadami.data.download.model.Download
import com.sf.tadami.data.download.model.EpisodeDownloadState
import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.domain.episode.Episode
import com.sf.tadami.ui.components.data.EpisodeItem
import org.junit.Assert.assertEquals
import org.junit.Test

class EpisodesDownloadedOnlyTest {

    private fun item(id: Long, seen: Boolean, state: Download.State = Download.State.NOT_DOWNLOADED) =
        EpisodeItem(Episode.create().copy(id = id, seen = seen), download = EpisodeDownloadState(state))

    private val episodes = listOf(
        item(1L, seen = true, state = Download.State.DOWNLOADED),
        item(2L, seen = false, state = Download.State.DOWNLOADED),
        item(3L, seen = false, state = Download.State.DOWNLOADING),
        item(4L, seen = false, state = Download.State.QUEUED),
        item(5L, seen = false),
    )

    private fun displayed(unseenFilter: Long?, downloadedOnly: Boolean) =
        episodes.filter { it.isDisplayed(unseenFilter, downloadedOnly) }.map { it.episode.id }

    @Test
    fun onlyTheDownloadedEpisodesAreDisplayed() {
        assertEquals(listOf(1L, 2L), displayed(null, downloadedOnly = true))
        assertEquals(listOf(1L, 2L, 3L, 4L, 5L), displayed(null, downloadedOnly = false))
    }

    @Test
    fun theSeenFilterCombinesWithIt() {
        assertEquals(listOf(2L), displayed(Anime.EPISODE_SHOW_UNSEEN, downloadedOnly = true))
        assertEquals(listOf(1L), displayed(Anime.EPISODE_SHOW_SEEN, downloadedOnly = true))
    }

    @Test
    fun selectAllOnlySelectsTheDownloadedEpisodes() {
        val result = episodes.selectDisplayed(null, downloadedOnly = true) { true }

        assertEquals(listOf(1L, 2L), result.filter { it.selected }.map { it.episode.id })
    }
}
