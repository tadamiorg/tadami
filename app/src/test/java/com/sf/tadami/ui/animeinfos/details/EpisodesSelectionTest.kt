package com.sf.tadami.ui.animeinfos.details

import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.domain.episode.Episode
import com.sf.tadami.ui.components.data.EpisodeItem
import org.junit.Assert.assertEquals
import org.junit.Test

class EpisodesSelectionTest {

    private fun item(id: Long, seen: Boolean, selected: Boolean = false) =
        EpisodeItem(Episode.create().copy(id = id, seen = seen), selected)

    private val episodes = listOf(
        item(1L, seen = true),
        item(2L, seen = false),
        item(3L, seen = true),
        item(4L, seen = false),
    )

    private fun List<EpisodeItem>.selectedIds() = filter { it.selected }.map { it.episode.id }

    @Test
    fun selectAllOnlySelectsTheEpisodesTheSeenFilterShows() {
        val result = episodes.selectDisplayed(Anime.EPISODE_SHOW_UNSEEN) { true }

        assertEquals(listOf(2L, 4L), result.selectedIds())
    }

    @Test
    fun invertOnlyInvertsTheDisplayedEpisodes() {
        val selected = episodes.map { it.copy(selected = it.episode.id == 1L) }

        val result = selected.selectDisplayed(Anime.EPISODE_SHOW_SEEN) { !it.selected }

        assertEquals(listOf(3L), result.selectedIds())
    }

    @Test
    fun withoutFilterEveryEpisodeIsSelected() {
        val result = episodes.selectDisplayed(null) { true }

        assertEquals(listOf(1L, 2L, 3L, 4L), result.selectedIds())
    }
}
