package com.sf.tadami.ui.animeinfos.episode.cast.channels

import android.util.Log
import com.google.android.gms.cast.Cast
import com.google.android.gms.cast.CastDevice
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

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