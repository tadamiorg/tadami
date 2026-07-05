package com.sf.tadami.ui.animeinfos.episode.cast

import android.annotation.SuppressLint
import android.util.Log
import android.widget.Toast
import com.google.android.gms.cast.CastStatusCodes
import com.google.android.gms.cast.MediaStatus
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastReasonCodes
import com.google.android.gms.cast.framework.CastSession
import com.sf.tadami.R
import com.sf.tadami.ui.animeinfos.episode.cast.channels.CustomCastChannel
import com.sf.tadami.ui.utils.UiToasts
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

fun showCastConnectionError(castContext: CastContext, error: Int) {
    when (castContext.getCastReasonCodeForCastStatusCode(error)) {
        CastReasonCodes.CAST_CANCELLED -> {
            // user cancelled the connection, nothing to report
        }
        CastReasonCodes.APPLICATION_LAUNCH_ERROR -> UiToasts.showToast(
            R.string.cast_error_app_not_installed,
            Toast.LENGTH_LONG
        )
        CastReasonCodes.NETWORK_ERROR, CastReasonCodes.CAST_SOCKET_ERROR -> UiToasts.showToast(
            R.string.cast_error_network,
            Toast.LENGTH_LONG
        )
        CastReasonCodes.CAST_TIMEOUT -> UiToasts.showToast(
            R.string.cast_error_timeout,
            Toast.LENGTH_LONG
        )
        else -> UiToasts.showToast(
            R.string.cast_error_connection_failed,
            Toast.LENGTH_LONG,
            CastStatusCodes.getStatusCodeString(error)
        )
    }
}

fun setCastCustomChannel(session : CastSession, channel : CustomCastChannel){
    try {
        session.setMessageReceivedCallbacks(
            channel.namespace,
            channel)
    } catch (e: IOException) {
        Log.d("CustomChannel", "Exception while creating channel", e)
    }
}

fun sendCastMessage(castSession: CastSession,channelNamespace : String,message: String) {
    try {
        castSession.sendMessage(channelNamespace, message)
            .setResultCallback { status ->
                if (!status.isSuccess) {
                    Log.d("CustomMessageSendFailed", "Sending message failed")
                }
            }
    } catch (e: Exception) {
        Log.d("CustomMessageSendFailed", "Exception while sending message", e)
    }
}