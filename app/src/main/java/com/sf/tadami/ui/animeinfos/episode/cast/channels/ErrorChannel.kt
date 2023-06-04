package com.sf.tadami.ui.animeinfos.episode.cast.channels

import com.google.android.gms.cast.CastDevice
import kotlinx.serialization.Serializable

class ErrorChannel : CustomCastChannel() {
    override val namespace: String
        get() = NAMESPACE

    override fun onMessageReceived(castDevice: CastDevice, namespace: String, message: String) {}

    companion object {
        const val NAMESPACE: String = "urn:x-cast:com.sf.tadami.error"
    }
}

enum class CastErrorCode(val code: Int) {
    COMMUNICATION(666),
    UNSUPPORTED(104),
    LOAD_FAILED(905)
}

@Serializable
data class TadamiCastError(
    val errorCode: Int
)