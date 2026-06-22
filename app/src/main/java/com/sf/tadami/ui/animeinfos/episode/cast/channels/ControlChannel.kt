package com.sf.tadami.ui.animeinfos.episode.cast.channels

import android.util.Log
import com.google.android.gms.cast.CastDevice
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Receives control messages from the `tadami-tv-cast` receiver: playback progress (so the phone
 * can persist watch time to its DB) and episode-navigation requests (next / previous / pick),
 * which the phone fulfils by re-resolving sources and re-loading them onto the TV.
 */
class ControlChannel(
    private val onControl: (TvControlMessage) -> Unit,
) : CustomCastChannel() {

    override val namespace: String
        get() = NAMESPACE

    override fun onMessageReceived(castDevice: CastDevice, namespace: String, message: String) {
        runCatching { json.decodeFromString<TvControlMessage>(message) }
            .onSuccess(onControl)
            .onFailure { Log.d("ControlChannel", "Failed to parse control message", it) }
    }

    companion object {
        const val NAMESPACE: String = "urn:x-cast:com.sf.tadami.control"
        private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    }
}

@Serializable
data class TvControlMessage(
    /** "progress" | "save" | "next" | "previous" | "selectEpisode" */
    val type: String,
    val position: Long = 0L,
    val duration: Long = 0L,
    val playing: Boolean = false,
    val episodeId: Long? = null,
)
