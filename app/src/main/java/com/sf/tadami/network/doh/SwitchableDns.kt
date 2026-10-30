package com.sf.tadami.network.doh

import okhttp3.Dns
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap

/**
 * Resolves with the DNS over HTTPS provider selected at each lookup, so the clients already built,
 * the extensions' ones included, follow a change without a restart.
 */
class SwitchableDns(
    private val providerId: () -> Int,
    private val providerDns: (id: Int) -> Dns?,
    private val system: Dns = Dns.SYSTEM,
) : Dns {

    private val resolvers = ConcurrentHashMap<Int, Dns>()

    override fun lookup(hostname: String): List<InetAddress> {
        val id = providerId()
        val dns = if (id == DohProviders.DISABLED) {
            system
        } else {
            // An unknown provider, stored by a newer version, resolves as if disabled
            resolvers.getOrPut(id) { providerDns(id) ?: system }
        }
        return dns.lookup(hostname)
    }
}
