package com.sf.tadami.domain.anime

import androidx.compose.ui.state.ToggleableState
import com.sf.tadami.source.model.SAnime
import com.sf.tadami.source.model.SAnimeStatus
import data.Anime as AnimeDb

data class Anime(
    val id: Long,
    val source: Long,
    val url: String,
    val title: String,
    val rawTitle: String?,
    val thumbnailUrl: String?,
    val release: String?,
    val studio: String?,
    val author: String?,
    val status: SAnimeStatus,
    val description: String?,
    val genres: List<String>?,
    val favorite: Boolean,
    val lastUpdate: Long,
    val nextUpdate: Long,
    val fetchInterval: Int,
    val initialized: Boolean,
    val episodeFlags : Long,
    var dateAdded: Long
) {
    fun copyFrom(other: SAnime): Anime {
        val otherRawTitle = other.rawTitle
        return this.copy(
            rawTitle = otherRawTitle ?: rawTitle,
            release = other.release,
            studio = other.studio,
            author = other.author,
            description = other.description,
            genres = other.genres,
            thumbnailUrl = other.thumbnailUrl,
            status = other.status,
            initialized = other.initialized && initialized,
        )
    }

    fun copyFrom(other: AnimeDb): Anime {
        var anime = this
        other.raw_title?.let { anime = anime.copy(rawTitle = it) }
        other.release?.let { anime = anime.copy(release = it) }
        other.studio?.let { anime = anime.copy(studio = it) }
        other.author?.let { anime = anime.copy(author = it) }
        other.description?.let { anime = anime.copy(description = it) }
        other.genres?.let { anime = anime.copy(genres = it) }
        other.thumbnail_url?.let { anime = anime.copy(thumbnailUrl = it) }
        anime = anime.copy(status = other.status ?: SAnimeStatus.UNKNOWN)
        if (!initialized) {
            anime = anime.copy(initialized = other.initialized)
        }
        return anime
    }

    val unseenFilterRaw: Long
        get() = episodeFlags and EPISODE_UNSEEN_MASK

    val unseenFilter: ToggleableState
        get() = when (unseenFilterRaw) {
            EPISODE_SHOW_UNSEEN -> ToggleableState.On
            EPISODE_SHOW_SEEN -> ToggleableState.Off
            else -> ToggleableState.Indeterminate
        }

    private val displayModeRaw: Long
        get() = episodeFlags and EPISODE_DISPLAY_MASK

    val displayMode : DisplayMode
        get() = DisplayMode.valueOf(displayModeRaw)

    val areEpisodesFiltered : Boolean
        get() = displayMode != DisplayMode.NAME || unseenFilter != ToggleableState.Indeterminate

    sealed class DisplayMode(
        val flag: Long,
    ) {
        object NAME : DisplayMode(EPISODE_DISPLAY_NAME)
        object NUMBER : DisplayMode(EPISODE_DISPLAY_NUMBER)

        companion object {
            private val types = setOf(NAME, NUMBER)
            fun valueOf(flags: Long): DisplayMode {
                return types.find { type -> type.flag == flags and EPISODE_DISPLAY_MASK } ?: NAME
            }
        }
    }

    companion object {
        const val SHOW_ALL = 0x00000000L

        const val EPISODE_SHOW_UNSEEN = 0x00000001L
        const val EPISODE_SHOW_SEEN = 0x00000002L
        const val EPISODE_UNSEEN_MASK = 0x00000003L

        const val EPISODE_DISPLAY_NAME = 0x00000000L
        const val EPISODE_DISPLAY_NUMBER = 0x00000040L
        const val EPISODE_DISPLAY_MASK = 0x00000040L

        fun create() = Anime(
            id = -1L,
            source = -1L,
            url = "",
            title = "",
            rawTitle = null,
            thumbnailUrl = null,
            release = null,
            studio = null,
            author = null,
            status = SAnimeStatus.UNKNOWN,
            description = null,
            genres = null,
            favorite = false,
            initialized = false,
            lastUpdate = 0L,
            nextUpdate = 0L,
            fetchInterval = 0,
            episodeFlags = 0L,
            dateAdded = 0L
        )
    }
}

fun SAnime.toDomainAnime(source: Long): Anime {
    return Anime.create().copy(
        source = source,
        url = url,
        title = title,
        rawTitle = rawTitle,
        thumbnailUrl = thumbnailUrl,
        release = release,
        studio = studio,
        author = author,
        status = status,
        description = description,
        genres = genres,
        initialized = initialized
    )
}

fun LibraryAnime.toAnime(): Anime {
    return Anime(
        id = id,
        source = source,
        url = url,
        title = title,
        rawTitle = rawTitle,
        thumbnailUrl = thumbnailUrl,
        release = release,
        studio = studio,
        author = author,
        status = status,
        description = description,
        genres = genres,
        favorite = favorite,
        initialized = initialized,
        fetchInterval = fetchInterval,
        lastUpdate = lastUpdate,
        nextUpdate = nextUpdate,
        episodeFlags = episodeFlags,
        dateAdded = dateAdded
    )
}

