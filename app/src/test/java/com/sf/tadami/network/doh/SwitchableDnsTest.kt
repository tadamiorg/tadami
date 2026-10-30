package com.sf.tadami.network.doh

import okhttp3.Dns
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.InetAddress

class SwitchableDnsTest {

    private fun fixed(ip: String) = Dns { listOf(InetAddress.getByName(ip)) }

    private val system = fixed("192.0.2.1")
    private val cloudflare = fixed("192.0.2.11")
    private val google = fixed("192.0.2.12")

    private var selected = DohProviders.DISABLED
    private val built = mutableListOf<Int>()

    private val dns = SwitchableDns(
        providerId = { selected },
        providerDns = { id ->
            built += id
            when (id) {
                1 -> cloudflare
                2 -> google
                else -> null
            }
        },
        system = system,
    )

    private fun resolved() = dns.lookup("example.com").single().hostAddress

    @Test
    fun theSystemResolvesWhileDohIsDisabled() {
        assertEquals("192.0.2.1", resolved())
        assertEquals(emptyList<Int>(), built)
    }

    @Test
    fun theSelectedProviderResolves() {
        selected = 1
        assertEquals("192.0.2.11", resolved())
    }

    @Test
    fun aChangeAppliesToTheNextLookup() {
        selected = 1
        assertEquals("192.0.2.11", resolved())
        selected = 2
        assertEquals("192.0.2.12", resolved())
        selected = DohProviders.DISABLED
        assertEquals("192.0.2.1", resolved())
    }

    @Test
    fun aProviderIsBuiltOnce() {
        selected = 1
        resolved()
        resolved()
        selected = 2
        resolved()
        selected = 1
        resolved()
        assertEquals(listOf(1, 2), built)
    }

    @Test
    fun anUnknownProviderFallsBackToTheSystem() {
        // A value stored by a newer version
        selected = 99
        assertEquals("192.0.2.1", resolved())
    }
}
