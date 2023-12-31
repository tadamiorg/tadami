package com.sf.tadami.domain.history

import java.util.Date

data class History(
    val id: Long,
    val episodeId: Long,
    val seenAt: Date?,
    val seenDuration: Long,
) {
    companion object {
        fun create() = History(
            id = -1L,
            episodeId = -1L,
            seenAt = null,
            seenDuration = -1L,
        )
    }
}