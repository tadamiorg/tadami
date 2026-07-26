package com.sf.tadami.ui.animeinfos.episode.cast

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.google.android.gms.cast.CastStatusCodes
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

/**
 * The phone's LAN IPv4 for the cast proxy base url. The cast receiver must be able to reach it, so
 * prefer the Wi-Fi (then Ethernet) network over whatever interface enumeration returns first —
 * a VPN's tun0 or the cellular rmnet address would be unreachable from the TV.
 */
fun getLocalIPAddress(context: Context): String? {
    // 1) The OS-declared Wi-Fi / Ethernet networks.
    runCatching {
        val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        for (transport in intArrayOf(NetworkCapabilities.TRANSPORT_WIFI, NetworkCapabilities.TRANSPORT_ETHERNET)) {
            @Suppress("DEPRECATION")
            for (network in connectivity.allNetworks) {
                val capabilities = connectivity.getNetworkCapabilities(network) ?: continue
                if (!capabilities.hasTransport(transport)) continue
                connectivity.getLinkProperties(network)?.linkAddresses
                    ?.map { it.address }
                    ?.firstOrNull { it is Inet4Address && !it.isLoopbackAddress }
                    ?.let { return it.hostAddress }
            }
        }
    }.onFailure { Log.w("getLocalIPAddress", "ConnectivityManager lookup failed", it) }

    // 2) Interface scan fallback (covers hotspot ap0/swlan0), skipping VPN/cellular interfaces.
    return runCatching {
        NetworkInterface.getNetworkInterfaces().asSequence()
            .filter { nic ->
                val name = nic.name.lowercase()
                !name.startsWith("tun") && !name.startsWith("ppp") && !name.startsWith("rmnet")
            }
            .sortedByDescending { it.name.lowercase().let { n -> n.startsWith("wlan") || n.startsWith("ap") || n.startsWith("swlan") } }
            .flatMap { it.inetAddresses.asSequence() }
            .firstOrNull { it is Inet4Address && !it.isLoopbackAddress && it.isSiteLocalAddress }
            ?.hostAddress
    }.getOrNull()
}

fun logCastConnectionError(source: String, error: Int) {
    Log.d("CastConnection", "$source failed with error $error (${CastStatusCodes.getStatusCodeString(error)})")
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