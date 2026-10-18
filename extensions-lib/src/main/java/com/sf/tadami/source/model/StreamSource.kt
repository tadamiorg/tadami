package com.sf.tadami.source.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import okhttp3.Headers

/**
 * A stream of an episode, what the player plays: HLS, DASH or a progressive file, recognized from its
 * content.
 *
 * @property url the URL of the stream
 * @property fullName shown in the player's list of streams
 * @property quality its quality, e.g. "1080p"
 * @property server the name of its host
 * @property headers sent with every request of the stream (its segments, keys...), a Referer for
 * instance
 * @property subtitleTracks its external subtitles
 * @property audioTracks its external audio tracks
 * @since extensions-lib 1.5
 */
@Serializable
data class StreamSource(
    val url : String = "",
    val fullName: String = "",
    val quality: String = "",
    val server: String = "",
    @Serializable(with = OkhttpHeadersSerializer::class)
    val headers: Headers? = null,
    val subtitleTracks: List<Track.SubtitleTrack> = emptyList(),
    val audioTracks: List<Track.AudioTrack> = emptyList()
)

/**
 * An external track of a [StreamSource].
 *
 * @property url the URL of the track
 * @property lang its language, as shown to the user
 * @since extensions-lib 1.5
 */
sealed class Track {
    abstract val url: String
    abstract val lang : String

    /**
     * A subtitle track.
     *
     * @property mimeType its format, see [com.sf.tadami.ui.utils.detectSubtitleFormat]
     * @since extensions-lib 1.5
     */
    @Serializable
    data class SubtitleTrack(
        override val url: String,
        override val lang: String,
        val mimeType: String = throw Exception("Stub !")
    ) : Track()

    /**
     * An audio track.
     *
     * @since extensions-lib 1.5
     */
    @Serializable
    data class AudioTrack(
        override val url: String,
        override val lang: String,
    ) : Track()
}

/**
 * Serializes the [StreamSource.headers].
 *
 * @since extensions-lib 1.5
 */
object OkhttpHeadersSerializer : KSerializer<Headers?> {
    override val descriptor: SerialDescriptor = throw Exception("Stub")
    override fun serialize(encoder: Encoder, value: Headers?): Unit = throw Exception("Stub")
    override fun deserialize(decoder: Decoder): Headers? = throw Exception("Stub")
}
