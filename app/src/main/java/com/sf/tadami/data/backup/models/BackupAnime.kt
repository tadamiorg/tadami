package com.sf.tadami.data.backup.models

import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.domain.anime.LibraryAnime
import com.sf.tadami.domain.episode.Episode
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
data class BackupAnime @OptIn(ExperimentalSerializationApi::class) constructor(
    @ProtoNumber(1) var source: String,
    @ProtoNumber(2) var url: String,
    @ProtoNumber(3) var title: String = "",
    @ProtoNumber(4) var thumbnailUrl: String? = null,
    @ProtoNumber(5) var release: String? = null,
    @ProtoNumber(6) var status: String? = null,
    @ProtoNumber(7) var description: String? = null,
    @ProtoNumber(8) var genres: List<String> = emptyList(),
    @ProtoNumber(9) var favorite: Boolean = true,
    @ProtoNumber(10) var initialized: Boolean = true,
    @ProtoNumber(20) var episodes: List<BackupEpisode> = emptyList(),

    ) {
    fun getAnimeImpl(): Anime {
        return Anime.create().copy(
            source = this@BackupAnime.source,
            url = this@BackupAnime.url,
            title = this@BackupAnime.title,
            thumbnailUrl = this@BackupAnime.thumbnailUrl,
            release = this@BackupAnime.release,
            status = this@BackupAnime.status,
            description = this@BackupAnime.description,
            genres = this@BackupAnime.genres,
            favorite = this@BackupAnime.favorite,
            initialized = this@BackupAnime.initialized
        )
    }

    fun getEpisodeImpl(): List<Episode> {
        return episodes.map {
            it.toEpisodeImpl()
        }
    }

    companion object {
        fun copyFrom(anime: LibraryAnime): BackupAnime {
            return BackupAnime(
                source = anime.source,
                url = anime.url,
                title = anime.title,
                thumbnailUrl = anime.thumbnailUrl,
                release = anime.release,
                status = anime.status,
                description = anime.description,
                genres = anime.genres.orEmpty(),
                favorite = anime.favorite,
                initialized = anime.initialized
            )
        }
    }
}