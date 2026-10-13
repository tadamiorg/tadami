package com.sf.tadami.ui.animeinfos.episode.cast

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.google.android.gms.cast.CastStatusCodes
import com.google.android.gms.cast.MediaStatus
import com.google.android.gms.cast.MediaTrack
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.sf.tadami.ui.animeinfos.episode.cast.channels.CustomCastChannel
import okio.IOException
import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * The shared CastContext, or null when Google Play services can't provide it (missing, disabled or
 * outdated, e.g. Huawei devices or de-Googled ROMs): cast is then unavailable instead of crashing.
 * Main thread only, like CastContext.getSharedInstance.
 */
fun castContextOrNull(context: Context): CastContext? {
    if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) != ConnectionResult.SUCCESS) {
        return null
    }
    return try {
        CastContext.getSharedInstance(context)
    } catch (e: Exception) {
        Log.w("CastContext", "Cast is unavailable", e)
        null
    }
}

/** The media played to its end (not stopped, replaced or failed). */
@SuppressLint("VisibleForTests")
fun isCastMediaFinished(playerState : Int?): Boolean {
    return playerState == MediaStatus.IDLE_REASON_FINISHED
}

/** The media is over, finished or failed: it can be played again. */
@SuppressLint("VisibleForTests")
fun isCastMediaEnded(playerState : Int?): Boolean {
    return isCastMediaFinished(playerState) || playerState == MediaStatus.IDLE_REASON_ERROR
}

/** The cast media's active subtitle track id: the first active id that is one of its text tracks. */
fun activeCastSubtitleTrackId(activeTrackIds: LongArray?, mediaTracks: List<MediaTrack>?): Long? {
    val textTrackIds = mediaTracks.orEmpty().filter { it.type == MediaTrack.TYPE_TEXT }.map { it.id }
    return activeTrackIds?.firstOrNull { it in textTrackIds }
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
                // A VPN network also lists the transports it runs over (Wi-Fi), but its address is the tunnel's
                if (!capabilities.hasTransport(transport) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) continue
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
            .filter { nic -> isLanInterfaceCandidate(nic.name) }
            .sortedByDescending { it.name.lowercase().let { n -> n.startsWith("wlan") || n.startsWith("ap") || n.startsWith("swlan") } }
            .flatMap { it.inetAddresses.asSequence() }
            .firstOrNull { it is Inet4Address && !it.isLoopbackAddress && it.isSiteLocalAddress }
            ?.hostAddress
    }.getOrNull()
}

/** Not a VPN tunnel (tun, ppp, WireGuard, IPsec) nor a cellular interface: the TV can't reach those addresses. */
internal fun isLanInterfaceCandidate(interfaceName: String): Boolean {
    val name = interfaceName.lowercase()
    return listOf("tun", "ppp", "wg", "ipsec", "rmnet").none { name.startsWith(it) }
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