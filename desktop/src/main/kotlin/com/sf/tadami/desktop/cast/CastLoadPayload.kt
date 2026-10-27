package com.sf.tadami.desktop.cast

import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.domain.episode.Episode
import com.sf.tadami.source.model.StreamSource
import com.sf.tadami.ui.animeinfos.episode.cast.CastProtocol
import com.sf.tadami.ui.animeinfos.episode.cast.castEpisodesWindow
import com.sf.tadami.ui.animeinfos.episode.cast.castSubtitleTrackId
import com.sf.tadami.ui.animeinfos.episode.cast.channels.CastSubtitleStyle
import com.sf.tadami.ui.animeinfos.episode.cast.proxy.proxiedImageUrl
import com.sf.tadami.ui.utils.convertToIetfLanguageTag
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

private val castJson = Json

/**
 * The LOAD request of [episode], the one the Android app's buildCastLoadRequest gives the Cast SDK, as
 * the JSON the SDK sends: the same media, tracks and customData, so that the web receiver plays it the
 * same way (header-bound sources through the proxy at [proxyBaseUrl]). Times are in seconds here.
 */
internal fun castLoadPayload(
    episode: Episode,
    anime: Anime?,
    availableSources: List<StreamSource>,
    selectedSource: StreamSource,
    episodes: List<Episode>,
    userAgent: String,
    subtitlePrefLanguages: List<String>,
    subtitlesEnabled: Boolean,
    resumeTimeMs: Long,
    proxyBaseUrl: String?,
    subtitleStyle: CastSubtitleStyle,
    contentUrl: String = selectedSource.url,
): JsonObject {
    val customData = buildJsonObject {
        put("minReceiverProtocol", CastProtocol.MIN_RECEIVER_VERSION)
        put("senderProtocol", CastProtocol.SENDER_VERSION)
        put("userAgent", userAgent)
        put("animeId", episode.animeId)
        put("episodeId", episode.id)
        put("seen", episode.seen)
        put("availableSources", castJson.encodeToString(availableSources))
        put("episodeUrl", episode.url)
        put("selectedSource", castJson.encodeToString(selectedSource))
        put(
            "episodes",
            buildJsonArray {
                castEpisodesWindow(episodes, episode.id).forEach { ep ->
                    addJsonObject {
                        put("id", ep.id)
                        put("name", ep.name)
                        put("episodeNumber", ep.episodeNumber.toDouble())
                        put("seen", ep.seen)
                    }
                }
            }.toString(),
        )
        put("displayMode", if (anime?.displayMode is Anime.DisplayMode.NAME) "NAME" else "NUMBER")
        proxyBaseUrl?.let { put("proxyBaseUrl", it) }
        put("subtitleStyle", castJson.encodeToString(subtitleStyle))
    }

    val subtitleTracks = selectedSource.subtitleTracks
    val trackNames = subtitleTracks.groupBy { it.lang }
        .flatMap { (lang, tracks) ->
            if (tracks.size > 1) tracks.mapIndexed { index, track -> track to "$lang #${index + 1}" } else tracks.map { it to lang }
        }.toMap()
    val activeTrackId = castSubtitleTrackId(subtitleTracks, subtitlePrefLanguages, subtitlesEnabled)

    return buildJsonObject {
        put("type", "LOAD")
        putJsonObject("media") {
            put("contentId", contentUrl)
            put("contentUrl", contentUrl)
            put("streamType", "BUFFERED")
            putJsonObject("metadata") {
                // MOVIE
                put("metadataType", 1)
                put("title", anime?.title ?: "Anime Title")
                put("subtitle", episode.name)
                anime?.thumbnailUrl?.let { cover ->
                    putJsonArray("images") { addJsonObject { put("url", proxiedImageUrl(proxyBaseUrl, cover) ?: cover) } }
                }
            }
            episode.totalTime.takeIf { it > 0L }?.let { put("duration", it / 1000.0) }
            if (subtitleTracks.isNotEmpty()) {
                putJsonArray("tracks") {
                    subtitleTracks.forEachIndexed { index, track ->
                        addJsonObject {
                            put("trackId", index + 1)
                            put("type", "TEXT")
                            put("subtype", "SUBTITLES")
                            put("name", trackNames[track] ?: "Subtitle ${index + 1}")
                            put("trackContentId", track.url)
                            put("trackContentType", track.mimeType)
                            put("language", track.lang.convertToIetfLanguageTag())
                        }
                    }
                }
            }
            putJsonObject("textTrackStyle") {
                put("foregroundColor", "#FFFFFFFF")
                put("backgroundColor", "#00000100")
                put("edgeType", "OUTLINE")
                put("edgeColor", "#000000F0")
                put("fontStyle", "BOLD")
            }
            put("customData", customData)
        }
        put("autoplay", true)
        put("currentTime", resumeTimeMs / 1000.0)
        activeTrackId?.let { putJsonArray("activeTrackIds") { add(JsonPrimitive(it)) } }
    }
}
