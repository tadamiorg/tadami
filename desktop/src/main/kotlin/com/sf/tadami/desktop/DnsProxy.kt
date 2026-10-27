package com.sf.tadami.desktop

import com.sf.tadami.network.DesktopDns
import com.sf.tadami.utils.Log
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URI
import java.util.concurrent.Executors
import kotlin.concurrent.thread

/**
 * A local HTTP proxy that resolves the hosts with the app's DNS ([DesktopDns]), for the network
 * stacks that only know the system's one: mpv and Chromium. HTTPS goes through CONNECT tunnels, still
 * encrypted end to end; a plain HTTP request is forwarded as it is, its connection closed after it.
 */
object DnsProxy {

    private const val TAG = "DnsProxy"
    private const val MAX_HEAD_BYTES = 64 * 1024
    private const val CONNECT_TIMEOUT_MILLIS = 15_000

    private val pool = Executors.newCachedThreadPool { Thread(it, "DnsProxy").apply { isDaemon = true } }

    /** `http://127.0.0.1:<port>`, the proxy started the first time. */
    val url: String by lazy {
        val server = ServerSocket(0, 64, InetAddress.getLoopbackAddress())
        thread(name = "DnsProxy", isDaemon = true) {
            while (true) {
                val client = runCatching { server.accept() }.getOrElse { break }
                pool.execute { serve(client) }
            }
        }
        "http://127.0.0.1:${server.localPort}"
    }

    private fun serve(client: Socket) {
        client.use {
            try {
                val input = client.getInputStream().buffered()
                val head = readHead(input) ?: return
                val lines = head.split("\r\n")
                val (method, target, version) = lines.first().split(" ").takeIf { it.size == 3 } ?: return
                if (method.equals("CONNECT", ignoreCase = true)) {
                    val upstream = connect(target, defaultPort = 443)
                    client.getOutputStream().write("HTTP/1.1 200 Connection Established\r\n\r\n".toByteArray())
                    relay(input, client, upstream)
                } else {
                    // The absolute-form of a plain HTTP request, asked to its server in its origin-form
                    val uri = URI(target)
                    val upstream = connect("${uri.host}:${uri.port.takeIf { it > 0 } ?: 80}", defaultPort = 80)
                    val path = (uri.rawPath?.ifEmpty { null } ?: "/") + (uri.rawQuery?.let { "?$it" } ?: "")
                    val headers = lines.drop(1).filter {
                        it.isNotEmpty() && !it.startsWith("proxy-connection:", ignoreCase = true) &&
                            !it.startsWith("connection:", ignoreCase = true)
                    }
                    val request = buildString {
                        append("$method $path $version\r\n")
                        headers.forEach { append(it).append("\r\n") }
                        append("Connection: close\r\n\r\n")
                    }
                    upstream.getOutputStream().write(request.toByteArray(Charsets.ISO_8859_1))
                    relay(input, client, upstream)
                }
            } catch (e: Exception) {
                Log.d(TAG, "Proxied connection failed: ${e.message}")
            }
        }
    }

    /** The request line and headers, up to the empty line; null if the client went away. */
    private fun readHead(input: InputStream): String? {
        val head = ByteArrayOutputStream()
        var matched = 0
        while (head.size() < MAX_HEAD_BYTES) {
            val byte = input.read()
            if (byte < 0) return null
            head.write(byte)
            matched = when {
                byte == '\r'.code && matched % 2 == 0 -> matched + 1
                byte == '\n'.code && matched % 2 == 1 -> matched + 1
                else -> 0
            }
            if (matched == 4) return head.toString(Charsets.ISO_8859_1.name()).removeSuffix("\r\n\r\n")
        }
        return null
    }

    private fun connect(authority: String, defaultPort: Int): Socket {
        val host = authority.substringBeforeLast(':').removePrefix("[").removeSuffix("]")
        val port = authority.substringAfterLast(':', "").toIntOrNull() ?: defaultPort
        // An IP is not a name to resolve
        val addresses = if (host.any { it.isLetter() } && !host.contains(':')) DesktopDns.lookup(host) else listOf(InetAddress.getByName(host))
        var last: Exception? = null
        for (address in addresses) {
            try {
                return Socket().apply { connect(InetSocketAddress(address, port), CONNECT_TIMEOUT_MILLIS) }
            } catch (e: Exception) {
                last = e
            }
        }
        throw last ?: IllegalStateException("No address for $host")
    }

    /** Both ways until one side closes: the other is closed then. */
    private fun relay(clientInput: InputStream, client: Socket, upstream: Socket) {
        upstream.use {
            val downstream = pool.submit {
                runCatching { upstream.getInputStream().copyTo(client.getOutputStream()) }
                runCatching { client.close() }
            }
            runCatching { clientInput.copyTo(upstream.getOutputStream()) }
            runCatching { upstream.close() }
            downstream.get()
        }
    }
}
