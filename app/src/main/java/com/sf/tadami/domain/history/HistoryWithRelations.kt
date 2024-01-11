package com.sf.tadami.domain.history

import java.util.Date

data class HistoryWithRelations(
    val id: Long,
    val episodeId: Long,
    val animeId: Long,
    val title: String,
    val episodeNumber: Double,
    val source : String,
    val seenAt: Date?,
    val thumbnailUrl: String?,
)