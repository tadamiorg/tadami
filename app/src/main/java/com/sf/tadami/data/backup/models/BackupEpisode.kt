package com.sf.tadami.data.backup.models

import com.sf.tadami.domain.episode.Episode
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class BackupEpisode(
    @ProtoNumber(1) var url: String,
    @ProtoNumber(2) var name: String,
    @ProtoNumber(3) var timeSeen: Long = 0,
    @ProtoNumber(4) var totalTime: Long = 0,
    @ProtoNumber(5) var dateFetch: Long = 0,
    @ProtoNumber(6) var dateUpload: Long = 0,
    @ProtoNumber(8) var episodeNumber: Float = 0F,
    @ProtoNumber(9) var seen: Boolean = false,
    @ProtoNumber(10) var sourceOrder: Long = 0,
    @ProtoNumber(11) var languages: String? = null,

) {
    fun toEpisodeImpl(): Episode {
        return Episode.create().copy(
            url = this@BackupEpisode.url,
            name = this@BackupEpisode.name,
            timeSeen = this@BackupEpisode.timeSeen,
            totalTime = this@BackupEpisode.totalTime,
            dateFetch = this@BackupEpisode.dateFetch,
            dateUpload = this@BackupEpisode.dateUpload,
            episodeNumber = this@BackupEpisode.episodeNumber,
            seen = this@BackupEpisode.seen,
            sourceOrder = this@BackupEpisode.sourceOrder,
            languages = this@BackupEpisode.languages
        )
    }
}