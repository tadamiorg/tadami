package com.sf.tadami.data.history

import com.sf.tadami.domain.history.History
import com.sf.tadami.domain.history.HistoryWithRelations
import java.util.Date

object HistoryMapper {

    fun mapHistory(
        id: Long,
        episodeId: Long,
        seenAt: Date?
    ): History = History(
        id = id,
        episodeId = episodeId,
        seenAt = seenAt
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
        seenAt: Date?
    ): HistoryWithRelations = HistoryWithRelations(
        id = historyId,
        episodeId = episodeId,
        animeId = animeId,
        title = title,
        episodeNumber = episodeNumber,
        seenAt = seenAt,
        source = sourceId,
        thumbnailUrl = thumbnailUrl
    )
}