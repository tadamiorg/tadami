package com.sf.tadami.source.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
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
        val mimeType: String
    ) : Track()

    @Serializable
    data class AudioTrack(
        override val url: String,
        override val lang: String,
    ) : Track()
}

object OkhttpHeadersSerializer : KSerializer<Headers?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Headers", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: Headers?) {
        val headersMap = value?.toMap() ?: emptyMap()
        encoder.encodeSerializableValue(MapSerializer(String.serializer(),String.serializer()), headersMap)
    }
    override fun deserialize(decoder: Decoder): Headers? {
        val headersMap = decoder.decodeSerializableValue(MapSerializer(String.serializer(),String.serializer()))
        if(headersMap.isEmpty()) return null
        return Headers.Builder().apply {
            headersMap.forEach { (name, value) ->
                add(name,value)
            }
        }.build()
    }
}