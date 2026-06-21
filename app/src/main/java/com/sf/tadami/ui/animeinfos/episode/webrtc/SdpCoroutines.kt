package com.sf.tadami.ui.animeinfos.episode.webrtc

import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private abstract class SetSdpObserver : SdpObserver {
    override fun onCreateSuccess(desc: SessionDescription?) {}
    override fun onCreateFailure(error: String?) {}
}

private abstract class CreateSdpObserver : SdpObserver {
    override fun onSetSuccess() {}
    override fun onSetFailure(error: String?) {}
}

suspend fun PeerConnection.createOfferSuspend(): SessionDescription =
    suspendCoroutine { cont ->
        createOffer(object : CreateSdpObserver() {
            override fun onCreateSuccess(desc: SessionDescription?) {
                if (desc != null) cont.resume(desc)
                else cont.resumeWithException(RuntimeException("createOffer returned null"))
            }

            override fun onCreateFailure(error: String?) =
                cont.resumeWithException(RuntimeException("createOffer failed: $error"))
        }, MediaConstraints())
    }

suspend fun PeerConnection.setLocalDescriptionSuspend(desc: SessionDescription): Unit =
    suspendCoroutine { cont ->
        setLocalDescription(object : SetSdpObserver() {
            override fun onSetSuccess() = cont.resume(Unit)
            override fun onSetFailure(error: String?) =
                cont.resumeWithException(RuntimeException("setLocalDescription failed: $error"))
        }, desc)
    }

suspend fun PeerConnection.setRemoteDescriptionSuspend(desc: SessionDescription): Unit =
    suspendCoroutine { cont ->
        setRemoteDescription(object : SetSdpObserver() {
            override fun onSetSuccess() = cont.resume(Unit)
            override fun onSetFailure(error: String?) =
                cont.resumeWithException(RuntimeException("setRemoteDescription failed: $error"))
        }, desc)
    }
