package com.sf.tadami.ui.animeinfos.episode.cast

import android.graphics.Color
import android.net.Uri
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaTrack
import com.google.android.gms.cast.TextTrackStyle
import com.google.android.gms.common.images.WebImage
import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.domain.episode.Episode
import com.sf.tadami.source.model.StreamSource
import com.sf.tadami.source.model.Track
import com.sf.tadami.ui.animeinfos.episode.cast.channels.CastSubtitleStyle
import com.sf.tadami.ui.animeinfos.episode.cast.proxy.proxiedImageUrl
import com.sf.tadami.ui.utils.convertToIetfLanguageTag
import kotlinx.serialization.json.Json
import org.json.JSONArray
import org.json.JSONObject

private val castJson = Json

/**
 * Builds the Cast load request for [episode]. Pure / app-context (no Activity or Compose), so it can
 * run both from EpisodeActivity (foreground) and from CastControlService (background). [themeJson] is
 * the pre-serialized theme object string reused as-is (or null); [resumeTimeMs] is the start position.
 *
 * All urls in the payload are RAW. The web receiver learns [proxyBaseUrl] from customData (and the
 * handshake) and routes its media requests through the phone proxy itself, at the network layer —
 * the payload stays format-agnostic and identical for both receivers (the native Terebi receiver
 * plays the raw `selectedSource` with its own header injection and ignores `proxyBaseUrl`).
 */
fun buildCastLoadRequest(
    episode: Episode,
    anime: Anime?,
    availableSources: List<StreamSource>,
    selectedSource: StreamSource,
    episodeUrl: String?,
    episodes: List<Episode>,
    displayMode: String,
    themeJson: String?,
    userAgent: String,
    subtitlePrefLanguages: List<String>,
    subtitlesEnabled: Boolean,
    resumeTimeMs: Long,
    proxyBaseUrl: String? = null,
    subtitleStyle: CastSubtitleStyle? = null,
): MediaLoadRequestData {
    val movieMetadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE)
    movieMetadata.putString(MediaMetadata.KEY_TITLE, anime?.title ?: "Anime Title")
    // Proxied so hotlink-protecting hosts serve it: neither the Cast SDK's image fetcher (sender
    // widget / media notification) nor the receiver's <img> sends a Referer. Falls back to raw.
    anime?.thumbnailUrl?.let { cover ->
        movieMetadata.addImage(WebImage(Uri.parse(proxiedImageUrl(proxyBaseUrl, cover) ?: cover)))
    }
    movieMetadata.putString(MediaMetadata.KEY_SUBTITLE, episode.name)

    val customData = JSONObject()
        // Compatibility handshake: the receiver forces an app update when its protocol < minReceiverProtocol.
        .put("minReceiverProtocol", CastProtocol.MIN_RECEIVER_VERSION)
        .put("senderProtocol", CastProtocol.SENDER_VERSION)
        .put("userAgent", userAgent)
        .put("animeId", episode.animeId)
        .put("episodeId", episode.id)
        .put("seen", episode.seen)
        .put("availableSources", castJson.encodeToString(availableSources))
        .put("episodeUrl", episodeUrl)
        .put("selectedSource", castJson.encodeToString(selectedSource))

    val episodesArray = JSONArray()
    episodes.forEach { ep ->
        episodesArray.put(
            JSONObject()
                .put("id", ep.id)
                .put("name", ep.name)
                .put("episodeNumber", ep.episodeNumber.toDouble())
                .put("seen", ep.seen)
        )
    }
    customData.put("episodes", episodesArray.toString())
    customData.put("displayMode", displayMode)
    themeJson?.let { customData.put("theme", it) }
    proxyBaseUrl?.let { customData.put("proxyBaseUrl", it) }
    // Carry the subtitle style with the load: the control-channel message only fires while the cast
    // player screen is composed, so without this a fresh load (or a notification-driven episode
    // switch) would render with the receiver's defaults instead of the user's preferences.
    subtitleStyle?.let { customData.put("subtitleStyle", castJson.encodeToString(it)) }

    val contentUrl = selectedSource.url
    val mediaInfosBuilder = MediaInfo.Builder(contentUrl)
        .setContentUrl(contentUrl)
        .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
        .setMetadata(movieMetadata)
        .setCustomData(customData)

    episode.totalTime.takeIf { it > 0L }?.let { mediaInfosBuilder.setStreamDuration(it) }

    var activeTrackIds: LongArray? = null
    val subtitleTracks = selectedSource.subtitleTracks
    if (subtitleTracks.isNotEmpty()) {
        val mediaTracks = ArrayList<MediaTrack>()
        val trackNameMap = subtitleTracks.groupBy { it.lang }
            .flatMap { (lang, tracks) ->
                if (tracks.size > 1) tracks.mapIndexed { index, track -> track to "$lang #${index + 1}" }
                else tracks.map { it to lang }
            }.toMap()
        subtitleTracks.forEachIndexed { index, track ->
            val trackId = (index + 1).toLong()
            mediaTracks.add(
                MediaTrack.Builder(trackId, MediaTrack.TYPE_TEXT)
                    .setName(trackNameMap[track] ?: "Subtitle ${index + 1}")
                    .setSubtype(MediaTrack.SUBTYPE_SUBTITLES)
                    .setContentId(track.url)
                    .setLanguage(track.lang.convertToIetfLanguageTag())
                    .setContentType(track.mimeType)
                    .build()
            )
        }
        if (mediaTracks.isNotEmpty()) {
            castSubtitleTrackId(subtitleTracks, subtitlePrefLanguages, subtitlesEnabled)
                ?.let { activeTrackIds = longArrayOf(it) }
            mediaInfosBuilder.setMediaTracks(mediaTracks)
        }
    }

    mediaInfosBuilder.setTextTrackStyle(
        TextTrackStyle().apply {
            foregroundColor = Color.argb(255, 255, 255, 255)
            backgroundColor = Color.argb(0, 0, 0, 1)
            edgeType = TextTrackStyle.EDGE_TYPE_OUTLINE
            edgeColor = Color.argb(240, 0, 0, 0)
            fontStyle = TextTrackStyle.FONT_STYLE_BOLD
        }
    )

    val mediaLoadRequestBuilder = MediaLoadRequestData.Builder()
        .setAutoplay(true)
        .setMediaInfo(mediaInfosBuilder.build())
        .setCurrentTime(resumeTimeMs)
    activeTrackIds?.let { mediaLoadRequestBuilder.setActiveTrackIds(it) }
    return mediaLoadRequestBuilder.build()
}

/**
 * The subtitle MediaTrack id (track index + 1) the load turns on, or null for none (subtitles turned
 * off in the settings, or no tracks): the first track matching the ordered language preference,
 * compared case-insensitively like the local player does, else the first track.
 */
internal fun castSubtitleTrackId(
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
