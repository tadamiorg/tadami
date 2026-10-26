package com.sf.tadami.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * The hosts whose Cloudflare challenge is being solved, for the screens waiting on them. A host
 * stays in the set until every solve that added it ended.
 */
class SolvingHosts {

    private val counts = HashMap<String, Int>()

    private val _hosts = MutableStateFlow<Set<String>>(emptySet())
    val hosts: StateFlow<Set<String>> = _hosts.asStateFlow()

    /** Runs [block] with [hosts] in the set, whatever its outcome. */
    fun <T> solving(hosts: Collection<String>, block: () -> T): T {
        val added = hosts.map { it.lowercase() }.toSet()
        add(added)
        try {
            return block()
        } finally {
            remove(added)
        }
    }

    @Synchronized
    private fun add(hosts: Set<String>) {
        hosts.forEach { counts[it] = (counts[it] ?: 0) + 1 }
        _hosts.value = counts.keys.toSet()
    }

    @Synchronized
    private fun remove(hosts: Set<String>) {
        hosts.forEach { host ->
            val count = (counts[host] ?: 0) - 1
            if (count > 0) counts[host] = count else counts.remove(host)
        }
        _hosts.value = counts.keys.toSet()
    }

    companion object {
        /**
         * Whether the source at [baseUrl] waits on one of [hosts]: its host, or another host of its
         * site, like a mirror on a subdomain.
         */
        fun isSolving(
            baseUrl: String?,
            hosts: Set<String>,
            siteOf: (String) -> String = ::registrableDomain,
        ): Boolean {
            if (hosts.isEmpty()) return false
            val host = baseUrl?.toHttpUrlOrNull()?.host ?: return false
            if (host in hosts) return true
            val site = siteOf(host)
            return hosts.any { siteOf(it) == site }
        }

        private fun registrableDomain(host: String): String =
            "http://$host/".toHttpUrlOrNull()?.topPrivateDomain() ?: host
    }
}
