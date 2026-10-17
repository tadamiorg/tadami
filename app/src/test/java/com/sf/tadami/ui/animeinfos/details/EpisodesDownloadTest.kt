package com.sf.tadami.ui.animeinfos.details

import com.sf.tadami.data.download.model.Download
import com.sf.tadami.data.download.model.EpisodeDownloadState
import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.domain.episode.Episode
import com.sf.tadami.ui.animeinfos.details.episodes.DetailsDownloadAction
import com.sf.tadami.ui.animeinfos.details.episodes.episodesToDownload
import com.sf.tadami.ui.components.data.EpisodeItem
import org.junit.Assert.assertEquals
import org.junit.Test

class EpisodesDownloadTest {

    private fun item(id: Long, seen: Boolean, state: Download.State = Download.State.NOT_DOWNLOADED) =
        EpisodeItem(Episode.create().copy(id = id, seen = seen), download = EpisodeDownloadState(state))

    // As the details screen lists them: the newest first
    private val episodes = listOf(
        item(6L, seen = false),
        item(5L, seen = false),
        item(4L, seen = false, state = Download.State.QUEUED),
        item(3L, seen = false, state = Download.State.DOWNLOADED),
        item(2L, seen = false),
        item(1L, seen = true),
    )

    private fun ids(action: DetailsDownloadAction, filter: Long? = null) =
        episodes.episodesToDownload(action, filter).map { it.id }

    @Test
    fun nextUnseenSkipsTheSeenTheDownloadedAndTheQueued() {
        assertEquals(listOf(2L), ids(DetailsDownloadAction.NEXT_1))
        assertEquals(listOf(2L, 5L, 6L), ids(DetailsDownloadAction.NEXT_5))
        assertEquals(listOf(2L, 5L, 6L), ids(DetailsDownloadAction.NEXT_10))
    }

    @Test
    fun unseenAndAllAreInWatchOrder() {
        assertEquals(listOf(2L, 3L, 4L, 5L, 6L), ids(DetailsDownloadAction.UNSEEN))
        assertEquals(listOf(1L, 2L, 3L, 4L, 5L, 6L), ids(DetailsDownloadAction.ALL))
    }

    @Test
    fun onlyTheEpisodesTheSeenFilterShows() {
        assertEquals(listOf(1L), ids(DetailsDownloadAction.ALL, Anime.EPISODE_SHOW_SEEN))
        assertEquals(emptyList<Long>(), ids(DetailsDownloadAction.NEXT_5, Anime.EPISODE_SHOW_SEEN))
        assertEquals(listOf(2L, 3L, 4L, 5L, 6L), ids(DetailsDownloadAction.ALL, Anime.EPISODE_SHOW_UNSEEN))
    }
}
