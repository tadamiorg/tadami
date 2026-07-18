package com.sf.tadami.data.backup.models

import com.sf.tadami.data.animeStatusAdapter
import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.domain.anime.LibraryAnime
import com.sf.tadami.domain.episode.Episode
import com.sf.tadami.domain.history.History
import com.sf.tadami.source.model.SAnimeStatus
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class BackupAnime(
    @ProtoNumber(1) var source: Long,
    @ProtoNumber(2) var url: String,
    @ProtoNumber(3) var title: String = "",
    @ProtoNumber(4) var thumbnailUrl: String? = null,
    @ProtoNumber(5) var release: String? = null,
    // Serialized as the SAnimeStatus enum name for backward/forward compatibility.
    @ProtoNumber(6) var status: String? = null,
    @ProtoNumber(7) var description: String? = null,
    @ProtoNumber(8) var genres: List<String> = emptyList(),
    @ProtoNumber(9) var favorite: Boolean = true,
    @ProtoNumber(10) var initialized: Boolean = true,
    @ProtoNumber(11) var episodeFlags: Int = 0,
    @ProtoNumber(12) var dateAdded: Long = 0,
    @ProtoNumber(13) var studio: String? = null,
    @ProtoNumber(14) var author: String? = null,
    @ProtoNumber(15) var rawTitle: String? = null,
    @ProtoNumber(20) var episodes: List<BackupEpisode> = emptyList(),
    @ProtoNumber(30) var history: List<BackupHistory> = emptyList(),

    ) {
    fun getAnimeImpl(): Anime {
        return Anime.create().copy(
            source = this@BackupAnime.source,
            url = this@BackupAnime.url,
            title = this@BackupAnime.title,
            rawTitle = this@BackupAnime.rawTitle,
            thumbnailUrl = this@BackupAnime.thumbnailUrl,
            release = this@BackupAnime.release,
            studio = this@BackupAnime.studio,
            author = this@BackupAnime.author,
            status = this@BackupAnime.status?.let(animeStatusAdapter::decode) ?: SAnimeStatus.UNKNOWN,
            description = this@BackupAnime.description,
            genres = this@BackupAnime.genres,
            favorite = this@BackupAnime.favorite,
            initialized = this@BackupAnime.initialized,
            episodeFlags = this@BackupAnime.episodeFlags.toLong(),
            dateAdded = this@BackupAnime.dateAdded
        )
    }

    fun getEpisodeImpl(): List<Episode> {
        return episodes.map {
            it.toEpisodeImpl()
        }
    }

    fun getHistoryImpl(): List<History> {
        return history.map {
            it.getHistoryImpl()
        }
    }

    companion object {
        fun copyFrom(anime: LibraryAnime): BackupAnime {
            return BackupAnime(
                source = anime.source,
                url = anime.url,
                title = anime.title,
                rawTitle = anime.rawTitle,
                thumbnailUrl = anime.thumbnailUrl,
                release = anime.release,
                studio = anime.studio,
                author = anime.author,
                status = anime.status.name,
                description = anime.description,
                genres = anime.genres.orEmpty(),
                favorite = anime.favorite,
                initialized = anime.initialized,
                episodeFlags = anime.episodeFlags.toInt(),
                dateAdded = anime.dateAdded
            )
        }
    }
}