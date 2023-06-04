package com.sf.tadami.ui.animeinfos.episode.cast.channels

import com.google.android.gms.cast.Cast
import com.google.android.gms.cast.CastDevice
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

abstract class CustomCastChannel : Cast.MessageReceivedCallback{
    abstract val namespace: String
}
inline fun <reified T>tadamiCastMessageCallback(crossinline onMessage : (device: CastDevice, namespace: String, message: T) -> Unit): Cast.MessageReceivedCallback {
    return Cast.MessageReceivedCallback { device, namespace, message ->
        val json = Json
        val parsedMessage = json.decodeFromString(serializer<T>(), message)
        onMessage(device, namespace, parsedMessage)
    }
}
