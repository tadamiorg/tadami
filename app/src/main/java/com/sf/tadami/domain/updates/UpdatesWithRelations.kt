package com.sf.tadami.domain.updates

data class UpdatesWithRelations(
    val animeId: Long,
    val animeTitle: String,
    val episodeId: Long,
    val episodeName: String,
    val seen: Boolean,
    val timeSeen: Long,
    val totalTime : Long,
    val sourceId: String,
    val dateFetch: Long,
    val thumbnailUrl: String?,
)