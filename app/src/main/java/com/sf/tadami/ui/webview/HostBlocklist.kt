package com.sf.tadami.ui.webview

import java.io.BufferedReader
import java.io.InputStream
import java.util.Locale

/**
 * Hosts-file style blocklist (`0.0.0.0 ads.example.com`) with exact host matching, the same
 * semantics a hosts file has.
 */
class HostBlocklist private constructor(private val hosts: Set<String>) {

    val size: Int get() = hosts.size

    fun isBlocked(host: String?): Boolean {
        if (host.isNullOrEmpty()) return false
        val normalized = host.lowercase(Locale.ROOT).trimEnd('.')
        if (NEVER_BLOCKED.any { normalized == it || normalized.endsWith(".$it") }) return false
        return normalized in hosts
    }

    companion object {
        /** Anti-bot challenges must always load, whatever the list says. */
        private val NEVER_BLOCKED = listOf("challenges.cloudflare.com", "hcaptcha.com", "recaptcha.net")

        val EMPTY = HostBlocklist(emptySet())

        fun parse(lines: Sequence<String>): HostBlocklist {
            val hosts = HashSet<String>()
            lines.forEach { raw ->
                val line = raw.substringBefore('#').trim()
                if (line.isEmpty()) return@forEach
                val parts = line.split(' ', '\t').filter { it.isNotEmpty() }
                // "0.0.0.0 host" or a bare "host"
                val host = if (parts.size >= 2) parts[1] else parts[0]
                hosts += host.lowercase(Locale.ROOT).trimEnd('.')
            }
            return HostBlocklist(hosts)
        }

        fun parse(stream: InputStream): HostBlocklist {
            return stream.bufferedReader().use { reader: BufferedReader -> parse(reader.lineSequence()) }
        }
    }
}
