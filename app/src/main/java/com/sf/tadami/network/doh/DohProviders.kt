package com.sf.tadami.network.doh

import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import java.net.InetAddress

/**
 * DNS over HTTPS resolvers the user can pick, with their ids, from Mihon.
 *
 * Each one is reached through fixed addresses, so the DNS blocking a host can't block it too.
 */
object DohProviders {

    const val DISABLED = -1

    class Provider(
        val id: Int,
        val name: String,
        val url: String,
        val bootstrapHosts: List<String>,
    )

    val all = listOf(
        Provider(
            id = 1,
            name = "Cloudflare",
            url = "https://cloudflare-dns.com/dns-query",
            bootstrapHosts = listOf(
                "162.159.36.1", "162.159.46.1", "1.1.1.1", "1.0.0.1", "162.159.132.53",
                "2606:4700:4700::1111", "2606:4700:4700::1001", "2606:4700:4700::0064", "2606:4700:4700::6400",
            ),
        ),
        Provider(
            id = 2,
            name = "Google",
            url = "https://dns.google/dns-query",
            bootstrapHosts = listOf("8.8.4.4", "8.8.8.8", "2001:4860:4860::8888", "2001:4860:4860::8844"),
        ),
        // AdGuard "Unfiltered", so no site is blocked
        Provider(
            id = 3,
            name = "AdGuard",
            url = "https://dns-unfiltered.adguard.com/dns-query",
            bootstrapHosts = listOf("94.140.14.140", "94.140.14.141", "2a10:50c0::1:ff", "2a10:50c0::2:ff"),
        ),
        Provider(
            id = 4,
            name = "Quad9",
            url = "https://dns.quad9.net/dns-query",
            bootstrapHosts = listOf("9.9.9.9", "149.112.112.112", "2620:fe::fe", "2620:fe::9"),
        ),
        Provider(
            id = 5,
            name = "AliDNS",
            url = "https://dns.alidns.com/dns-query",
            bootstrapHosts = listOf("223.5.5.5", "223.6.6.6", "2400:3200::1", "2400:3200:baba::1"),
        ),
        Provider(
            id = 6,
            name = "DNSPod",
            url = "https://doh.pub/dns-query",
            bootstrapHosts = listOf("1.12.12.12", "120.53.53.53"),
        ),
        Provider(
            id = 7,
            name = "360",
            url = "https://doh.360.cn/dns-query",
            bootstrapHosts = listOf(
                "101.226.4.6", "218.30.118.6", "123.125.81.6", "140.207.198.6",
                "180.163.249.75", "101.199.113.208", "36.99.170.86",
            ),
        ),
        Provider(
            id = 8,
            name = "Quad 101",
            url = "https://dns.twnic.tw/dns-query",
            bootstrapHosts = listOf("101.101.101.101", "2001:de4::101", "2001:de4::102"),
        ),
        // Without ad blocking
        Provider(
            id = 9,
            name = "Mullvad",
            url = "https://dns.mullvad.net/dns-query",
            bootstrapHosts = listOf("194.242.2.2", "2a07:e340::2"),
        ),
        // Unfiltered
        Provider(
            id = 10,
            name = "Control D",
            url = "https://freedns.controld.com/p0",
            bootstrapHosts = listOf("76.76.2.0", "76.76.10.0", "2606:1a40::", "2606:1a40:1::"),
        ),
        Provider(
            id = 11,
            name = "Njalla",
            url = "https://dns.njal.la/dns-query",
            bootstrapHosts = listOf("95.215.19.53", "2001:67c:2354:2::53"),
        ),
        Provider(
            id = 12,
            name = "Shecan",
            url = "https://free.shecan.ir/dns-query",
            bootstrapHosts = listOf("178.22.122.100", "185.51.200.2"),
        ),
    )

    /** The resolver of the provider [id], querying it with [client], or null when DoH is disabled */
    fun dns(id: Int, client: OkHttpClient): Dns? {
        val provider = all.firstOrNull { it.id == id } ?: return null
        return DnsOverHttps.Builder()
            .client(client)
            .url(provider.url.toHttpUrl())
            // IP literals: no lookup
            .bootstrapDnsHosts(provider.bootstrapHosts.map(InetAddress::getByName))
            .build()
    }
}
