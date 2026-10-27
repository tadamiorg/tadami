package com.sf.tadami.data.interactors.history

import com.sf.tadami.data.history.HistoryRepository
import com.sf.tadami.domain.episode.Episode
import com.sf.tadami.domain.history.HistoryUpdate
import java.util.Date

class UpdateHistoryInteractor (
    private val historyRepository: HistoryRepository,
){
    suspend fun awaitAnimeHistoryUpdate(
        episode: Episode
    ) {
        historyRepository.upsertHistory(
            HistoryUpdate(
                episodeId = episode.id,
                seenAt = Date()
            )
        )
    }
}