package com.sf.tadami.ui.animeinfos.episode.webrtc

import com.sf.tadami.source.model.StreamSource
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Phone (sender) side of the Tadami-TV WebRTC protocol. Field names + @SerialName +
 * the "t" class discriminator are byte-identical to the TV app's WireMessage so the
 * two decoders interop. Sources are sent as the existing [StreamSource] whose JSON
 * (headers serialized as a Map via OkhttpHeadersSerializer) matches the TV's
 * WireStreamSource exactly.
 */

const val TV_PROTOCOL_VERSION = 1

val tvProtocolJson: Json = Json {
    classDiscriminator = "t"
    ignoreUnknownKeys = true
    explicitNulls = false
    encodeDefaults = true
}

@Serializable
data class WireAnime(
    val title: String = "",
    val thumbnailUrl: String? = null,
    val displayMode: String = "NUMBER",
)

@Serializable
data class WireEpisode(
    val id: Long,
    val animeId: Long = 0L,
    val url: String = "",
    val name: String = "",
    val episodeNumber: Float = 0f,
    val seen: Boolean = false,
)

@Serializable
sealed class WireMessage {
    abstract val id: Long
}

@Serializable
@SerialName("HELLO")
data class Hello(override val id: Long, val client: String, val protocol: Int = TV_PROTOCOL_VERSION) : WireMessage()

@Serializable
@SerialName("LOAD")
data class Load(
    override val id: Long,
    val anime: WireAnime,
    val episode: WireEpisode,
    val episodes: List<WireEpisode> = emptyList(),
    val selectedSource: StreamSource,
    val availableSources: List<StreamSource> = emptyList(),
    val resumeTimeMs: Long = 0L,
    val selectedSubtitleIndex: Int? = null,
    val userAgentFallback: String? = null,
    val autoplay: Boolean = true,
) : WireMessage()

@Serializable
@SerialName("SET_SUBTITLE")
data class SetSubtitle(override val id: Long, val subtitleIndex: Int?) : WireMessage()

@Serializable
@SerialName("FETCHING")
data class Fetching(override val id: Long, val what: String) : WireMessage()

@Serializable
@SerialName("ERROR")
data class ErrorMessage(override val id: Long, val code: String, val message: String? = null) : WireMessage()

@Serializable
@SerialName("PING")
data class Ping(override val id: Long) : WireMessage()

@Serializable
@SerialName("READY")
data class Ready(override val id: Long, val device: String, val protocol: Int = TV_PROTOCOL_VERSION) : WireMessage()

@Serializable
@SerialName("SELECT_SOURCE")
data class SelectSource(override val id: Long, val sourceUrl: String, val server: String = "", val quality: String = "") : WireMessage()

@Serializable
@SerialName("SELECT_EPISODE")
data class SelectEpisode(override val id: Long, val episodeId: Long) : WireMessage()

@Serializable
@SerialName("NEXT_EPISODE")
data class NextEpisode(override val id: Long) : WireMessage()

@Serializable
@SerialName("PREV_EPISODE")
data class PrevEpisode(override val id: Long) : WireMessage()

@Serializable
@SerialName("SELECT_SUBTITLE_REQ")
data class SelectSubtitleReq(override val id: Long, val subtitleIndex: Int?) : WireMessage()

@Serializable
@SerialName("PROGRESS")
data class Progress(
    override val id: Long,
    val positionMs: Long,
    val durationMs: Long,
    val playing: Boolean,
    val buffering: Boolean = false,
    val ended: Boolean = false,
) : WireMessage()

@Serializable
@SerialName("ENDED")
data class Ended(override val id: Long) : WireMessage()

@Serializable
@SerialName("PONG")
data class Pong(override val id: Long) : WireMessage()

@Serializable
data class SignalEnvelope(
    val type: String,
    val code: String? = null,
    val client: String? = null,
    val sdp: String? = null,
    val candidate: String? = null,
    val sdpMid: String? = null,
    val sdpMLineIndex: Int? = null,
    val reason: String? = null,
    val v: Int? = null,
)

object SignalType {
    const val PAIR = "PAIR"
    const val PAIR_OK = "PAIR_OK"
    const val PAIR_FAIL = "PAIR_FAIL"
    const val OFFER = "OFFER"
    const val ANSWER = "ANSWER"
    const val ICE = "ICE"
    const val BUSY = "BUSY"
    const val DONE = "DONE"
}

fun encodeMessage(message: WireMessage): String =
    tvProtocolJson.encodeToString(WireMessage.serializer(), message)

fun decodeMessage(text: String): WireMessage =
    tvProtocolJson.decodeFromString(WireMessage.serializer(), text)
