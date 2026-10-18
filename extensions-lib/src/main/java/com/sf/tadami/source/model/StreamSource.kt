package com.sf.tadami.source.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import okhttp3.Headers

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

sealed class Track {
    abstract val url: String
    abstract val lang : String

    @Serializable
    data class SubtitleTrack(
        override val url: String,
        override val lang: String,
        val mimeType: String = throw Exception("Stub !")
    ) : Track()

    @Serializable
    data class AudioTrack(
        override val url: String,
        override val lang: String,
    ) : Track()
}





object OkhttpHeadersSerializer : KSerializer<Headers?> {
    override val descriptor: SerialDescriptor = throw Exception("Stub")
    override fun serialize(encoder: Encoder, value: Headers?): Unit = throw Exception("Stub")
    override fun deserialize(decoder: Decoder): Headers? = throw Exception("Stub")
}