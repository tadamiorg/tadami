package com.sf.tadami.ui.animeinfos.episode.cast

import com.sf.tadami.domain.episode.Episode
import com.sf.tadami.source.model.Track
import com.sf.tadami.ui.utils.convertToIetfLanguageTag

/**
 * The episodes sent with the load: the ones around [currentId] rather than the whole list. A cast
 * message is limited to 64KB, which a long series (1000+ episodes) went past, failing the load. The
 * receivers only show this list and step through it (next/previous), which the window keeps working.
 */
public fun castEpisodesWindow(
    episodes: List<Episode>,
    currentId: Long,
    radius: Int = CAST_EPISODES_RADIUS,
): List<Episode> {
    val index = episodes.indexOfFirst { it.id == currentId }.coerceAtLeast(0)
    return episodes.subList((index - radius).coerceAtLeast(0), (index + radius + 1).coerceAtMost(episodes.size))
}

private const val CAST_EPISODES_RADIUS = 100

/**
 * The subtitle MediaTrack id (track index + 1) the load turns on, or null for none (subtitles turned
 * off in the settings, or no tracks): the first track matching the ordered language preference,
 * compared case-insensitively like the local player does, else the first track.
 */
public fun castSubtitleTrackId(
    subtitleTracks: List<Track.SubtitleTrack>,
    subtitlePrefLanguages: List<String>,
    subtitlesEnabled: Boolean,
): Long? {
    if (!subtitlesEnabled || subtitleTracks.isEmpty()) return null
    val matchIndex = subtitlePrefLanguages.firstNotNullOfOrNull { lang ->
        subtitleTracks.indexOfFirst { it.lang.convertToIetfLanguageTag().equals(lang, ignoreCase = true) }
            .takeIf { it >= 0 }
    }
    return (matchIndex ?: 0) + 1L
}
