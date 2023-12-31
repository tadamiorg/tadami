package com.sf.tadami.data.history

import com.sf.tadami.domain.history.History
import com.sf.tadami.domain.history.HistoryWithRelations
import java.util.Date

object HistoryMapper {

    fun mapHistory(
        id: Long,
        episodeId: Long,
        seenAt: Date?,
        seenDuration: Long,
    ): History = History(
        id = id,
        episodeId = episodeId,
        seenAt = seenAt,
        seenDuration = seenDuration,
    )

    fun mapHistoryWithRelations(
        historyId: Long,
        animeId: Long,
        episodeId: Long,
        title: String,
        thumbnailUrl: String?,
        sourceId: String,
        isFavorite: Boolean,
        episodeNumber: Double,
        seenAt: Date?,
        seenDuration: Long,
    ): HistoryWithRelations = HistoryWithRelations(
        id = historyId,
        episodeId = episodeId,
        animeId = animeId,
        title = title,
        episodeNumber = episodeNumber,
        seenAt = seenAt,
        seenDuration = seenDuration,
        thumbnailUrl = thumbnailUrl
    )
}