package com.sf.tadami.network

import com.sf.tadami.utils.Log
import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * The DNS of the desktop app: Cloudflare's DNS over HTTPS, the system's one when it can't answer (a
 * LAN name, DoH blocked). ISPs block video hosts in their DNS (vidmoly.me resolves to 127.0.0.1 in
 * France), which the phones get around with their private DNS.
 */
object DesktopDns : Dns {

    private const val TAG = "DesktopDns"
    private val CACHE_MILLIS = TimeUnit.MINUTES.toMillis(5)

    private val doh: Dns by lazy {
        DnsOverHttps.Builder()
            .client(OkHttpClient.Builder().build())
            .url("https://cloudflare-dns.com/dns-query".toHttpUrl())
            .bootstrapDnsHosts(InetAddress.getByName("1.1.1.1"), InetAddress.getByName("1.0.0.1"))
            .build()
    }

    private val cache = ConcurrentHashMap<String, Pair<List<InetAddress>, Long>>()

    override fun lookup(hostname: String): List<InetAddress> {
        val now = System.currentTimeMillis()
        cache[hostname]?.takeIf { (_, at) -> now - at < CACHE_MILLIS }?.let { return it.first }
        val addresses = try {
            doh.lookup(hostname).ifEmpty { Dns.SYSTEM.lookup(hostname) }
        } catch (e: Exception) {
            Log.d(TAG, "DNS over HTTPS has no answer for $hostname, the system's DNS then: ${e.message}")
            Dns.SYSTEM.lookup(hostname)
        }
        cache[hostname] = addresses to now
        return addresses
    }
}
