package com.sf.tadami.ui.animeinfos.episode.cast

import android.annotation.SuppressLint
import android.util.Log
import com.google.android.gms.cast.MediaStatus
import com.google.android.gms.cast.framework.CastSession
import com.sf.tadami.ui.animeinfos.episode.cast.channels.CustomCastChannel
import okio.IOException
import java.net.Inet4Address
import java.net.NetworkInterface

@SuppressLint("VisibleForTests")
fun isCastMediaFinished(playerState : Int?): Boolean {
    return playerState == MediaStatus.IDLE_REASON_FINISHED || playerState == MediaStatus.IDLE_REASON_ERROR
}

fun getLocalIPAddress(): String? {
    try {
        val en = NetworkInterface.getNetworkInterfaces()
        while (en.hasMoreElements()) {
            val networkInterface = en.nextElement()
            val enu = networkInterface.inetAddresses
            while (enu.hasMoreElements()) {
                val inetAddress = enu.nextElement()
                if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                    return inetAddress.getHostAddress()
                }
            }
        }
    } catch (ex: Exception) {
        ex.printStackTrace()
    }

    return null
}

fun setCastCustomChannel(session : CastSession, channel : CustomCastChannel){
    try {
        session.setMessageReceivedCallbacks(
            channel.namespace,
            channel)
    } catch (e: IOException) {
        Log.e("CustomChannel", "Exception while creating channel", e)
    }
}

fun sendCastMessage(castSession: CastSession,channelNamespace : String,message: String) {
    try {
        castSession.sendMessage(channelNamespace, message)
            .setResultCallback { status ->
                if (!status.isSuccess) {
                    Log.e("CustomMessageSendFailed", "Sending message failed")
                }
            }
    } catch (e: Exception) {
        Log.e("CustomMessageSendFailed", "Exception while sending message", e)
    }
}