package com.sf.tadami.ui.animeinfos.episode.webrtc

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.ArrayDeque

const val NSD_SERVICE_TYPE = "_tadamitv._tcp"

data class DiscoveredTv(
    val name: String,
    val host: String,
    val port: Int,
)

/**
 * Discovers Tadami-TV receivers on the LAN. Resolves are serialized through a queue
 * to dodge NsdManager's "listener already in use" limitation on API <= 33.
 */
class NsdDiscovery(context: Context) {

    private val appContext = context.applicationContext
    private val nsdManager = appContext.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val wifiManager = appContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private var multicastLock: WifiManager.MulticastLock? = null

    private val _devices = MutableStateFlow<List<DiscoveredTv>>(emptyList())
    val devices: StateFlow<List<DiscoveredTv>> = _devices

    private val resolveQueue = ArrayDeque<NsdServiceInfo>()
    private var resolving = false
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    fun start() {
        if (discoveryListener != null) return
        acquireMulticastLock()
        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {}
            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                if (serviceInfo.serviceType?.contains("tadamitv") == true) {
                    enqueueResolve(serviceInfo)
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                _devices.value = _devices.value.filterNot { it.name == serviceInfo.serviceName }
            }

            override fun onDiscoveryStopped(serviceType: String) {}
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e("NsdDiscovery", "start failed: $errorCode")
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
        }
        discoveryListener = listener
        runCatching {
            nsdManager.discoverServices(NSD_SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
        }
    }

    @Synchronized
    private fun enqueueResolve(info: NsdServiceInfo) {
        resolveQueue.add(info)
        resolveNext()
    }

    @Synchronized
    private fun resolveNext() {
        if (resolving) return
        val info = resolveQueue.poll() ?: return
        resolving = true
        nsdManager.resolveService(info, object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                synchronized(this@NsdDiscovery) {
                    resolving = false
                    resolveNext()
                }
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                @Suppress("DEPRECATION")
                val host = serviceInfo.host?.hostAddress
                if (host != null) {
                    val tv = DiscoveredTv(serviceInfo.serviceName, host, serviceInfo.port)
                    _devices.value = (_devices.value.filterNot { it.name == tv.name } + tv)
                }
                synchronized(this@NsdDiscovery) {
                    resolving = false
                    resolveNext()
                }
            }
        })
    }

    fun stop() {
        discoveryListener?.let { runCatching { nsdManager.stopServiceDiscovery(it) } }
        discoveryListener = null
        synchronized(this) {
            resolveQueue.clear()
            resolving = false
        }
        _devices.value = emptyList()
        multicastLock?.let { runCatching { if (it.isHeld) it.release() } }
        multicastLock = null
    }

    private fun acquireMulticastLock() {
        if (multicastLock != null) return
        multicastLock = wifiManager.createMulticastLock("tadami-tv-discovery").apply {
            setReferenceCounted(true)
            runCatching { acquire() }
        }
    }
}
