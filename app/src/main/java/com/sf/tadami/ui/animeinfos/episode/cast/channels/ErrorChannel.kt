package com.sf.tadami.ui.animeinfos.episode.cast.channels

import android.util.Log
import com.google.android.gms.cast.CastDevice
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class ErrorChannel : CustomCastChannel() {
    override val namespace: String
        get() = NAMESPACE

    // Always-on logcat sink for receiver-reported errors (`adb logcat -s CastReceiverError`) —
    // the CastVideoPlayer snackbar only exists while that screen is composed.
    override fun onMessageReceived(castDevice: CastDevice, namespace: String, message: String) {
        val error = runCatching { tolerantJson.decodeFromString<TadamiCastError>(message) }.getOrNull()
        if (error != null) {
            Log.e(TAG, "receiver errorCode=${error.errorCode}" + (error.detail?.let { " detail=$it" } ?: ""))
        } else {
            Log.e(TAG, "unparseable receiver error: $message")
        }
    }

    companion object {
        const val NAMESPACE: String = "urn:x-cast:com.sf.tadami.error"
        const val TAG: String = "CastReceiverError"
        private val tolerantJson = Json { ignoreUnknownKeys = true }
    }
}

enum class CastErrorCode(val code: Int) {
    COMMUNICATION(666),
    UNSUPPORTED(104),
    LOAD_FAILED(905)
}

@Serializable
data class TadamiCastError(
    val errorCode: Int,
    /** Receiver diagnostic (shaka category/code + error data — failing URL, HTTP status). */
    val detail: String? = null,
)
