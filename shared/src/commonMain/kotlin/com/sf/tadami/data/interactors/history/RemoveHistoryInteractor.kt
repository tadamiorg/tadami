package com.sf.tadami.data.interactors.history

import com.sf.tadami.data.history.HistoryRepository
import com.sf.tadami.domain.history.HistoryWithRelations

class RemoveHistoryInteractor(
    private val repository: HistoryRepository,
) {
    suspend fun awaitAll(): Boolean {
        return repository.deleteAllHistory()
    }

    suspend fun await(history: HistoryWithRelations) {
        repository.resetHistory(history.id)
    }

    suspend fun await(animeId: Long) {
        repository.resetHistoryByAnimeId(animeId)
    }

}