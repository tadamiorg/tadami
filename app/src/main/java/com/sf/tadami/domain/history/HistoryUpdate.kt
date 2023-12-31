package com.sf.tadami.domain.history

import java.util.Date

data class HistoryUpdate(
    val episodeId: Long,
    val seenAt: Date,
    val sessionSeenDuration: Long,
)