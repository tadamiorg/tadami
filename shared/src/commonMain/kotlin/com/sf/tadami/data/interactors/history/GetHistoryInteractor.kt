package com.sf.tadami.data.interactors.history

import com.sf.tadami.data.history.HistoryRepository
import com.sf.tadami.domain.history.History
import com.sf.tadami.domain.history.HistoryWithRelations
import kotlinx.coroutines.flow.Flow

class GetHistoryInteractor(
    private val repository: HistoryRepository,
) {

    suspend fun await(animeId: Long): List<History> {
        return repository.getHistoryByAnimeId(animeId)
    }

    fun subscribe(query: String): Flow<List<HistoryWithRelations>> {
        return repository.getHistory(query)
    }
}