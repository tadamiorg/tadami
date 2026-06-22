package com.sf.tadami.ui.animeinfos.episode.cast.channels

import android.util.Log
import com.google.android.gms.cast.CastDevice
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Receives crash logs pushed by the `tadami-tv-cast` receiver over a custom Cast channel, so the
 * phone can let the user export them (the TV has no share sheet of its own).
 */
class CrashChannel(
    private val onCrashLog: (TvCrashLog) -> Unit,
) : CustomCastChannel() {

    override val namespace: String
        get() = NAMESPACE

    override fun onMessageReceived(castDevice: CastDevice, namespace: String, message: String) {
        runCatching { json.decodeFromString<TvCrashLog>(message) }
            .onSuccess(onCrashLog)
            .onFailure { Log.d("CrashChannel", "Failed to parse crash log", it) }
    }

    companion object {
        const val NAMESPACE: String = "urn:x-cast:com.sf.tadami.crash"
        private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    }
}

@Serializable
data class TvCrashLog(
    val stacktrace: String = "",
    val packageName: String = "",
    val versionName: String = "",
    val timestamp: Long = 0L,
)
