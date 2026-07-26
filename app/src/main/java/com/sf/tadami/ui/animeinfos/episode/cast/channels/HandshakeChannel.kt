package com.sf.tadami.ui.animeinfos.episode.cast.channels

import android.util.Log
import com.google.android.gms.cast.CastDevice
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Sender↔receiver compatibility handshake channel. The phone pushes its protocol versions (and the
 * proxy base url) to the receiver as soon as a cast session connects, before any media is loaded.
 * The native Tadami Terebi receiver only consumes those messages; the *web* receiver additionally
 * replies with a [ReceiverHandshakeMessage] identifying itself — that reply is how the phone knows
 * it is casting to the web receiver (proxy playback, one-time notice dialog).
 */
class HandshakeChannel(
    private val onReply: (ReceiverHandshakeMessage) -> Unit = {},
) : CustomCastChannel() {
    override val namespace: String
        get() = NAMESPACE

    override fun onMessageReceived(castDevice: CastDevice, namespace: String, message: String) {
        runCatching { json.decodeFromString<ReceiverHandshakeMessage>(message) }
            .onSuccess(onReply)
            .onFailure { Log.d("HandshakeChannel", "Unparseable handshake reply", it) }
    }

    companion object {
        const val NAMESPACE: String = "urn:x-cast:com.sf.tadami.handshake"
        private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    }
}

/** Receiver → phone handshake reply. Only the web receiver sends this. */
@Serializable
data class ReceiverHandshakeMessage(
    /** "web" (the native receiver never replies). */
    val receiverType: String = "",
    val receiverProtocol: Int = 0,
    val receiverVersion: String = "",
)
