package com.sf.tadami.ui.animeinfos.episode.cast.channels

import com.sf.tadami.utils.Log
import com.google.android.gms.cast.CastDevice
import com.sf.tadami.ui.animeinfos.episode.cast.preview.CastPreviewProtocol
import com.sf.tadami.ui.animeinfos.episode.cast.preview.CastPreviewRequest
import com.sf.tadami.ui.animeinfos.episode.cast.preview.CastPreviewSource
import kotlinx.serialization.json.Json

/**
 * Receives control messages from the `tadami-tv-cast` receiver: playback progress (so the phone
 * can persist watch time to its DB) and episode-navigation requests (next / previous / pick),
 * which the phone fulfils by re-resolving sources and re-loading them onto the TV.
 */
class ControlChannel(
    /** Seekbar preview requests of the web receiver ([CastPreviewProtocol]), apart from the other messages. */
    private val onPreviewRequest: (CastPreviewRequest) -> Unit = {},
    /** The stream the web receiver plays, for the seekbar preview prefetch. */
    private val onPreviewSource: (CastPreviewSource) -> Unit = {},
    private val onControl: (TvControlMessage) -> Unit,
) : CustomCastChannel() {

    override val namespace: String
        get() = NAMESPACE

    override fun onMessageReceived(castDevice: CastDevice, namespace: String, message: String) {
        runCatching { json.decodeFromString<TvControlMessage>(message) }
            .onSuccess { control ->
                when (control.type) {
                    CastPreviewProtocol.TYPE_REQUEST -> CastPreviewProtocol.decodeRequest(message)?.let(onPreviewRequest)
                        ?: Log.d("ControlChannel", "Unusable preview request")
                    CastPreviewProtocol.TYPE_SOURCE -> CastPreviewProtocol.decodeSource(message)?.let(onPreviewSource)
                        ?: Log.d("ControlChannel", "Unusable preview source")
                    else -> onControl(control)
                }
            }
            .onFailure { Log.d("ControlChannel", "Failed to parse control message", it) }
    }

    companion object {
        const val NAMESPACE: String = "urn:x-cast:com.sf.tadami.control"
        private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    }
}
