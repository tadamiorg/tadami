package com.sf.tadami.desktop.cast

import com.sf.tadami.utils.Log
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.security.cert.X509Certificate
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlin.concurrent.thread

/**
 * A connection to a Cast device (Cast v2: length-prefixed protobuf `CastMessage`s over TLS), the part
 * of the Google Cast SDK the desktop needs: JSON messages on namespaces, between this sender and the
 * device (`receiver-0`) or the app it runs. The device's certificate is its own, not a CA's: any is
 * accepted, as the SDK senders do on the LAN.
 */
internal class CastChannel(
    host: String,
    port: Int,
    private val onMessage: (Message) -> Unit,
    private val onClosed: () -> Unit,
) : Closeable {

    data class Message(val source: String, val destination: String, val namespace: String, val payload: String)

    private val socket: SSLSocket = trustAll().socketFactory.createSocket().let { it as SSLSocket }.apply {
        connect(InetSocketAddress(host, port), CONNECT_TIMEOUT_MILLIS)
        startHandshake()
    }
    private val input = DataInputStream(socket.inputStream.buffered())
    private val output = DataOutputStream(socket.outputStream.buffered())

    @Volatile
    private var closed = false

    // The device drops a sender that stops pinging
    private val heartbeat = Executors.newSingleThreadScheduledExecutor { Thread(it, "CastHeartbeat").apply { isDaemon = true } }

    init {
        thread(name = "CastChannel", isDaemon = true) { readLoop() }
        heartbeat.scheduleWithFixedDelay(
            { runCatching { send(RECEIVER, NS_HEARTBEAT, """{"type":"PING"}""") } },
            HEARTBEAT_SECONDS, HEARTBEAT_SECONDS, TimeUnit.SECONDS,
        )
    }

    @Synchronized
    fun send(destination: String, namespace: String, payload: String) {
        if (closed) throw IOException("Cast channel closed")
        val bytes = encode(Message(SENDER, destination, namespace, payload))
        output.writeInt(bytes.size)
        output.write(bytes)
        output.flush()
    }

    private fun readLoop() {
        try {
            while (!closed) {
                val length = input.readInt()
                if (length !in 0..MAX_MESSAGE_BYTES) throw IOException("Cast message of $length bytes")
                val bytes = ByteArray(length).also { input.readFully(it) }
                val message = decode(bytes) ?: continue
                if (message.namespace == NS_HEARTBEAT && message.payload.contains("\"PING\"")) {
                    send(message.source, NS_HEARTBEAT, """{"type":"PONG"}""")
                    continue
                }
                runCatching { onMessage(message) }.onFailure { Log.e(TAG, "Cast message not handled", it) }
            }
        } catch (e: Exception) {
            if (!closed) Log.w(TAG, "Cast connection lost: ${e.message}")
        } finally {
            val wasOpen = !closed
            close()
            if (wasOpen) onClosed()
        }
    }

    override fun close() {
        closed = true
        heartbeat.shutdownNow()
        runCatching { socket.close() }
    }

    companion object {
        const val SENDER = "sender-tadami"
        const val RECEIVER = "receiver-0"
        const val NS_CONNECTION = "urn:x-cast:com.google.cast.tp.connection"
        const val NS_HEARTBEAT = "urn:x-cast:com.google.cast.tp.heartbeat"
        const val NS_RECEIVER = "urn:x-cast:com.google.cast.receiver"
        const val NS_MEDIA = "urn:x-cast:com.google.cast.media"

        private const val TAG = "CastChannel"
        private const val CONNECT_TIMEOUT_MILLIS = 5000
        private const val HEARTBEAT_SECONDS = 5L
        private const val MAX_MESSAGE_BYTES = 1 shl 20

        private fun trustAll(): SSLContext = SSLContext.getInstance("TLS").apply {
            val trustAll = object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
                override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
            }
            init(null, arrayOf<TrustManager>(trustAll), null)
        }

        // CastMessage: 1 protocol_version (0 = CASTV2_1_0), 2 source_id, 3 destination_id, 4 namespace,
        // 5 payload_type (0 = STRING), 6 payload_utf8, 7 payload_binary
        fun encode(message: Message): ByteArray = ByteArrayOutputStream().apply {
            writeVarintField(1, 0)
            writeStringField(2, message.source)
            writeStringField(3, message.destination)
            writeStringField(4, message.namespace)
            writeVarintField(5, 0)
            writeStringField(6, message.payload)
        }.toByteArray()

        /** The string message in [bytes], null for a binary one (none the app needs). */
        fun decode(bytes: ByteArray): Message? {
            val fields = HashMap<Int, String>()
            var binary = false
            var index = 0
            fun varint(): Long {
                var result = 0L
                var shift = 0
                while (true) {
                    val byte = bytes[index++].toInt()
                    result = result or ((byte and 0x7F).toLong() shl shift)
                    if (byte and 0x80 == 0) return result
                    shift += 7
                }
            }
            while (index < bytes.size) {
                val key = varint().toInt()
                val field = key ushr 3
                when (key and 0x7) {
                    0 -> if (field == 5 && varint() != 0L) binary = true
                    2 -> {
                        val length = varint().toInt()
                        fields[field] = String(bytes, index, length, Charsets.UTF_8)
                        index += length
                    }
                    else -> throw IOException("Unexpected protobuf wire type in a cast message")
                }
            }
            if (binary) return null
            return Message(
                source = fields[2].orEmpty(),
                destination = fields[3].orEmpty(),
                namespace = fields[4].orEmpty(),
                payload = fields[6].orEmpty(),
            )
        }

        private fun ByteArrayOutputStream.writeVarint(value: Long) {
            var rest = value
            while (rest and 0x7FL.inv() != 0L) {
                write(((rest and 0x7F) or 0x80).toInt())
                rest = rest ushr 7
            }
            write(rest.toInt())
        }

        private fun ByteArrayOutputStream.writeVarintField(field: Int, value: Long) {
            writeVarint((field shl 3).toLong())
            writeVarint(value)
        }

        private fun ByteArrayOutputStream.writeStringField(field: Int, value: String) {
            val bytes = value.toByteArray(Charsets.UTF_8)
            writeVarint(((field shl 3) or 2).toLong())
            writeVarint(bytes.size.toLong())
            write(bytes)
        }
    }
}
