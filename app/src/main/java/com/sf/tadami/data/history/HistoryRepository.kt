package com.sf.tadami.data.history

import android.util.Log
import com.sf.tadami.data.DataBaseHandler
import com.sf.tadami.domain.history.History
import com.sf.tadami.domain.history.HistoryUpdate
import com.sf.tadami.domain.history.HistoryWithRelations
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {

    fun getHistory(query: String): Flow<List<HistoryWithRelations>>

    suspend fun getLastHistory(): HistoryWithRelations?

    suspend fun getHistoryByAnimeId(animeId: Long): List<History>

    suspend fun resetHistory(historyId: Long)

    suspend fun resetHistoryByAnimeId(animeId: Long)

    suspend fun deleteAllHistory(): Boolean

    suspend fun upsertHistory(historyUpdate: HistoryUpdate)
}

class HistoryRepositoryImpl(
    private val handler: DataBaseHandler,
) : HistoryRepository {

    override fun getHistory(query: String): Flow<List<HistoryWithRelations>> {
        return handler.subscribeToList {
            historyViewQueries.history(query, HistoryMapper::mapHistoryWithRelations)
        }
    }

    override suspend fun getLastHistory(): HistoryWithRelations? {
        return handler.awaitOneOrNull {
            historyViewQueries.getLatestHistory(HistoryMapper::mapHistoryWithRelations)
        }
    }

    override suspend fun getHistoryByAnimeId(animeId: Long): List<History> {
        return handler.awaitList { historyQueries.getHistoryByAnimeId(animeId, HistoryMapper::mapHistory) }
    }

    override suspend fun resetHistory(historyId: Long) {
        try {
            handler.await { historyQueries.resetHistoryById(historyId) }
        } catch (e: Exception) {
            Log.e("ResetHistory",e.stackTraceToString())
        }
    }

    override suspend fun resetHistoryByAnimeId(animeId: Long) {
        try {
            handler.await { historyQueries.resetHistoryByAnimeId(animeId) }
        } catch (e: Exception) {
            Log.e("ResetHistoryByAnimeId",e.stackTraceToString())
        }
    }

    override suspend fun deleteAllHistory(): Boolean {
        return try {
            handler.await { historyQueries.removeAllHistory() }
            true
        } catch (e: Exception) {
            Log.e("DeleteAllHistory",e.stackTraceToString())
            false
        }
    }

    override suspend fun upsertHistory(historyUpdate: HistoryUpdate) {
        try {
            handler.await {
                historyQueries.upsert(
                    historyUpdate.episodeId,
                    historyUpdate.seenAt,
                )
            }
        } catch (e: Exception) {
            Log.e("UpsertHistory",e.stackTraceToString())
        }
    }
}
