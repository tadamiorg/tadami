package com.sf.tadami.ui.animeinfos.episode.webrtc

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.RtpReceiver
import org.webrtc.SessionDescription
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

enum class RtcState { NEW, CONNECTING, CONNECTED, DISCONNECTED, FAILED, CLOSED }

/**
 * Phone is the WebRTC OFFERER. It creates the data channel, makes the offer, and
 * drives the remote TV player by sending [WireMessage]s once the channel is OPEN.
 */
class WebRtcSenderSession(
    private val onLocalIceCandidate: (IceCandidate) -> Unit,
) {
    private val _incoming = MutableSharedFlow<WireMessage>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val incoming: SharedFlow<WireMessage> = _incoming

    private val _state = MutableStateFlow(RtcState.NEW)
    val state: StateFlow<RtcState> = _state

    private var peer: PeerConnection? = null
    private var dataChannel: DataChannel? = null

    private val dcObserver = object : DataChannel.Observer {
        override fun onBufferedAmountChange(previousAmount: Long) {}
        override fun onStateChange() {
            when (dataChannel?.state()) {
                DataChannel.State.OPEN -> _state.value = RtcState.CONNECTED
                DataChannel.State.CLOSED -> _state.value = RtcState.CLOSED
                else -> {}
            }
        }

        override fun onMessage(buffer: DataChannel.Buffer) {
            if (buffer.binary) return
            val bytes = ByteArray(buffer.data.remaining())
            buffer.data.get(bytes)
            val text = String(bytes, StandardCharsets.UTF_8)
            runCatching { decodeMessage(text) }.getOrNull()?.let { _incoming.tryEmit(it) }
        }
    }

    private val pcObserver = object : PeerConnection.Observer {
        override fun onIceCandidate(candidate: IceCandidate) = onLocalIceCandidate(candidate)
        override fun onDataChannel(dc: DataChannel) {} // we are the offerer; we create it

        override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState) {
            when (newState) {
                PeerConnection.IceConnectionState.CONNECTED,
                PeerConnection.IceConnectionState.COMPLETED ->
                    if (_state.value != RtcState.CONNECTED) _state.value = RtcState.CONNECTING

                PeerConnection.IceConnectionState.DISCONNECTED -> _state.value = RtcState.DISCONNECTED
                PeerConnection.IceConnectionState.FAILED -> _state.value = RtcState.FAILED
                PeerConnection.IceConnectionState.CLOSED -> _state.value = RtcState.CLOSED
                else -> {}
            }
        }

        override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
        override fun onIceConnectionReceivingChange(receiving: Boolean) {}
        override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
        override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
        override fun onAddStream(stream: MediaStream?) {}
        override fun onRemoveStream(stream: MediaStream?) {}
        override fun onRenegotiationNeeded() {}
        override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {}
    }

    /** Creates the peer + data channel and returns the local offer SDP. */
    suspend fun createOffer(): String {
        val config = PeerConnection.RTCConfiguration(emptyList()).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }
        val pc = WebRtcEngine.factory.createPeerConnection(config, pcObserver)
            ?: error("Failed to create PeerConnection")
        peer = pc
        _state.value = RtcState.CONNECTING

        val init = DataChannel.Init().apply {
            ordered = true
            negotiated = false
        }
        val dc = pc.createDataChannel("tadami-control", init)
        dataChannel = dc
        dc.registerObserver(dcObserver)

        val offer = pc.createOfferSuspend()
        pc.setLocalDescriptionSuspend(offer)
        return offer.description
    }

    suspend fun handleAnswer(answerSdp: String) {
        peer?.setRemoteDescriptionSuspend(SessionDescription(SessionDescription.Type.ANSWER, answerSdp))
    }

    fun addRemoteIceCandidate(candidate: IceCandidate) {
        peer?.addIceCandidate(candidate)
    }

    fun send(message: WireMessage): Boolean {
        val dc = dataChannel ?: return false
        if (dc.state() != DataChannel.State.OPEN) return false
        val bytes = encodeMessage(message).toByteArray(StandardCharsets.UTF_8)
        return dc.send(DataChannel.Buffer(ByteBuffer.wrap(bytes), false))
    }

    fun close() {
        runCatching { dataChannel?.unregisterObserver() }
        runCatching { dataChannel?.close() }
        runCatching { dataChannel?.dispose() }
        runCatching { peer?.close() }
        runCatching { peer?.dispose() }
        dataChannel = null
        peer = null
        _state.value = RtcState.CLOSED
    }
}
