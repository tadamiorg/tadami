package com.sf.tadami.ui.animeinfos.episode.cast

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Process-wide state of the current cast session's receiver. With Cast Connect the same app id
 * launches either the native Tadami Terebi app (installed on the TV) or the web receiver
 * (everything else). Only the web receiver replies to the protocol handshake, so its reply — parsed
 * by [CastControlService][com.sf.tadami.notifications.cast.CastControlService] via
 * `HandshakeChannel` — is what flips [receiverType] to WEB. Absence of a reply means NATIVE.
 */
object CastSessionState {

    enum class ReceiverType {
        /** No handshake reply (yet) — behaves exactly like NATIVE. */
        UNKNOWN,
        NATIVE,
        WEB,
    }

    val receiverType = MutableStateFlow(ReceiverType.UNKNOWN)

    /** Live base url of the phone proxy ("http://<ip>:<port>"), published when the proxy (re)starts. */
    val proxyBaseUrl = MutableStateFlow<String?>(null)

    /** One-shot UI event: show the "you're on the web receiver" notice dialog. */
    val showWebReceiverNotice = MutableStateFlow(false)

    fun clear() {
        receiverType.value = ReceiverType.UNKNOWN
        proxyBaseUrl.value = null
        showWebReceiverNotice.value = false
    }
}
