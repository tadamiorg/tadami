package com.sf.tadami.ui.tabs.updates

import com.sf.tadami.domain.updates.UpdatesWithRelations
import org.junit.Assert.assertEquals
import org.junit.Test

class UpdatesDownloadedOnlyTest {

    private fun update(episodeId: Long) = UpdatesWithRelations(
        animeId = episodeId / 10, animeTitle = "Anime", episodeId = episodeId, episodeName = "Episode $episodeId",
        seen = false, timeSeen = 0L, totalTime = 0L, sourceId = 1L, dateFetch = 0L, thumbnailUrl = null,
    )

    private val updates = listOf(update(11L), update(12L), update(21L))

    @Test
    fun offEveryUpdateStays() {
        assertEquals(listOf(11L, 12L, 21L), updates.downloadedOnly(null).map { it.episodeId })
    }

    @Test
    fun onlyTheDownloadedEpisodesStay() {
        assertEquals(listOf(12L, 21L), updates.downloadedOnly(setOf(12L, 21L, 30L)).map { it.episodeId })
        assertEquals(emptyList<Long>(), updates.downloadedOnly(emptySet()).map { it.episodeId })
    }
}
