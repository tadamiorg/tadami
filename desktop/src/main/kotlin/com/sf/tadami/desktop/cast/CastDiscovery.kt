package com.sf.tadami.desktop.cast

import com.sf.tadami.utils.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import javax.jmdns.JmDNS
import javax.jmdns.ServiceEvent
import javax.jmdns.ServiceInfo
import javax.jmdns.ServiceListener
import kotlin.concurrent.thread

/** A Cast device of the LAN: a Chromecast, a Google TV, a speaker group... */
data class CastDevice(val id: String, val name: String, val model: String, val host: String, val port: Int)

/** Finds the Cast devices of the LAN as the SDK does: they announce `_googlecast._tcp` over mDNS. */
internal object CastDiscovery {

    private const val TAG = "CastDiscovery"
    private const val SERVICE_TYPE = "_googlecast._tcp.local."

    private val found = MutableStateFlow<List<CastDevice>>(emptyList())
    val devices: StateFlow<List<CastDevice>> = found.asStateFlow()

    @Volatile
    private var jmdns: JmDNS? = null

    /** Starts listening, once: the devices keep coming and going while the app runs. */
    @Synchronized
    fun start() {
        if (jmdns != null) return
        val address = lanAddress()?.let(InetAddress::getByName) ?: run {
            Log.w(TAG, "No LAN address: no cast devices")
            return
        }
        // Opening the multicast socket takes a moment
        thread(name = "CastDiscovery", isDaemon = true) {
            runCatching {
                JmDNS.create(address).also { jmdns = it }.addServiceListener(SERVICE_TYPE, listener)
            }.onFailure { Log.e(TAG, "mDNS discovery failed", it) }
        }
    }

    private val listener = object : ServiceListener {
        override fun serviceAdded(event: ServiceEvent) {
            event.dns.requestServiceInfo(event.type, event.name, true)
        }

        override fun serviceRemoved(event: ServiceEvent) {
            val id = event.info?.getPropertyString("id") ?: event.name
            found.update { devices -> devices.filterNot { it.id == id } }
        }

        override fun serviceResolved(event: ServiceEvent) {
            val device = event.info.toDevice() ?: return
            found.update { devices -> (devices.filterNot { it.id == device.id } + device).sortedBy { it.name.lowercase() } }
        }
    }

    private fun ServiceInfo.toDevice(): CastDevice? {
        val host = inet4Addresses.firstOrNull()?.hostAddress ?: return null
        return CastDevice(
            id = getPropertyString("id") ?: name,
            name = getPropertyString("fn") ?: name,
            model = getPropertyString("md").orEmpty(),
            host = host,
            port = port,
        )
    }
}

/**
 * The address of this computer on the LAN: the one of its route to the internet, not the one of a
 * virtual adapter (VPN, WSL, Hyper-V...). No packet is sent, a UDP socket only picks its route.
 */
internal fun lanAddress(): String? = runCatching {
    DatagramSocket().use { socket ->
        socket.connect(InetSocketAddress("8.8.8.8", 53))
        (socket.localAddress as? Inet4Address)?.takeUnless { it.isAnyLocalAddress || it.isLoopbackAddress }?.hostAddress
    }
}.getOrNull()
