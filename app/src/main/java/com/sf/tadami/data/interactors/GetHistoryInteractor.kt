package com.sf.tadami.data.interactors

import com.sf.tadami.data.history.HistoryRepository
import com.sf.tadami.domain.history.History
import com.sf.tadami.domain.history.HistoryWithRelations
import kotlinx.coroutines.flow.Flow

class GetHistoryInteractor(
    private val repository: HistoryRepository,
) {

    suspend fun await(mangaId: Long): List<History> {
        return repository.getHistoryByAnimeId(mangaId)
    }

    fun subscribe(query: String): Flow<List<HistoryWithRelations>> {
        return repository.getHistory(query)
    }
}