package com.sf.tadami.ui.animeinfos.episode.webrtc

import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.webrtc.IceCandidate
import java.io.BufferedWriter
import java.net.InetSocketAddress
import java.net.Socket

enum class SenderStatus { IDLE, CONNECTING, PAIR_FAILED, CONNECTED, DISCONNECTED, ERROR }

/**
 * Phone-side controller: discovers TVs (NSD), runs the offerer handshake over the TV's
 * signaling socket (pairing code -> SDP/ICE), and exposes the connected
 * [WebRtcSenderSession] once the data channel is open.
 */
class WebRtcSender(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val discovery = NsdDiscovery(appContext)
    val devices: StateFlow<List<DiscoveredTv>> get() = discovery.devices

    private val _status = MutableStateFlow(SenderStatus.IDLE)
    val status: StateFlow<SenderStatus> = _status

    private val _session = MutableStateFlow<WebRtcSenderSession?>(null)
    val session: StateFlow<WebRtcSenderSession?> = _session

    private var socket: Socket? = null

    fun startDiscovery() = discovery.start()
    fun stopDiscovery() = discovery.stop()

    fun connect(device: DiscoveredTv, code: String) {
        if (_status.value == SenderStatus.CONNECTING || _status.value == SenderStatus.CONNECTED) return
        WebRtcEngine.ensureInitialized(appContext)
        _status.value = SenderStatus.CONNECTING
        scope.launch { runHandshake(device, code) }
    }

    private fun runHandshake(device: DiscoveredTv, code: String) {
        var session: WebRtcSenderSession? = null
        try {
            val sock = Socket()
            sock.connect(InetSocketAddress(device.host, device.port), 5000)
            socket = sock
            val reader = sock.getInputStream().bufferedReader()
            val writer = sock.getOutputStream().bufferedWriter()

            writeEnvelope(writer, SignalEnvelope(type = SignalType.PAIR, code = code, client = deviceName(), v = 1))

            var line = reader.readLine()
            while (line != null) {
                val env = runCatching { tvProtocolJson.decodeFromString(SignalEnvelope.serializer(), line) }.getOrNull()
                if (env == null) { line = reader.readLine(); continue }
                when (env.type) {
                    SignalType.PAIR_OK -> {
                        val s = WebRtcSenderSession(onLocalIceCandidate = { c ->
                            writeEnvelope(
                                writer,
                                SignalEnvelope(
                                    type = SignalType.ICE,
                                    candidate = c.sdp,
                                    sdpMid = c.sdpMid,
                                    sdpMLineIndex = c.sdpMLineIndex,
                                ),
                            )
                        })
                        session = s
                        bindSession(s, writer)
                        val offer = kotlinx.coroutines.runBlocking { s.createOffer() }
                        writeEnvelope(writer, SignalEnvelope(type = SignalType.OFFER, sdp = offer))
                    }

                    SignalType.PAIR_FAIL -> {
                        _status.value = SenderStatus.PAIR_FAILED
                        break
                    }

                    SignalType.BUSY -> {
                        _status.value = SenderStatus.ERROR
                        break
                    }

                    SignalType.ANSWER -> {
                        val sdp = env.sdp
                        if (sdp != null) kotlinx.coroutines.runBlocking { session?.handleAnswer(sdp) }
                    }

                    SignalType.ICE -> {
                        session?.addRemoteIceCandidate(
                            IceCandidate(env.sdpMid, env.sdpMLineIndex ?: 0, env.candidate ?: ""),
                        )
                    }

                    SignalType.DONE -> break
                    else -> {}
                }
                line = reader.readLine()
            }
        } catch (e: Exception) {
            Log.e("WebRtcSender", "handshake error", e)
            if (_status.value != SenderStatus.CONNECTED) _status.value = SenderStatus.ERROR
        }
    }

    private fun bindSession(s: WebRtcSenderSession, writer: BufferedWriter) {
        scope.launch {
            s.state.collect { st ->
                when (st) {
                    RtcState.CONNECTED -> {
                        _session.value = s
                        _status.value = SenderStatus.CONNECTED
                        runCatching { writeEnvelope(writer, SignalEnvelope(type = SignalType.DONE)) }
                    }

                    RtcState.FAILED, RtcState.CLOSED, RtcState.DISCONNECTED -> {
                        if (_session.value === s) {
                            _session.value = null
                            _status.value = SenderStatus.DISCONNECTED
                        }
                    }

                    else -> {}
                }
            }
        }
    }

    fun disconnect() {
        _session.value?.close()
        _session.value = null
        runCatching { socket?.close() }
        socket = null
        _status.value = SenderStatus.IDLE
    }

    fun stop() {
        disconnect()
        discovery.stop()
    }

    private fun writeEnvelope(writer: BufferedWriter, env: SignalEnvelope) {
        val json = tvProtocolJson.encodeToString(SignalEnvelope.serializer(), env)
        synchronized(writer) {
            writer.write(json)
            writer.write("\n")
            writer.flush()
        }
    }

    private fun deviceName(): String = Build.MODEL?.takeIf { it.isNotBlank() } ?: "Phone"
}
