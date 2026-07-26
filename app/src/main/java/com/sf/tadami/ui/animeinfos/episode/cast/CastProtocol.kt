package com.sf.tadami.ui.animeinfos.episode.cast

import com.sf.tadami.ui.animeinfos.episode.cast.CastProtocol.MIN_RECEIVER_VERSION


/**
 * Phone↔TV cast compatibility contract. This is a monotonic protocol version, independent of the
 * marketing versionName, bumped only when the wire contract between the sender (this phone app) and
 * the Tadami Terebi receiver changes in a way an older receiver can't honor (new customData keys,
 * new control-channel messages, etc.).
 *
 * The phone advertises [MIN_RECEIVER_VERSION] in the cast load customData; the receiver compares it
 * against the version it implements and forces the user to update the TV app when it is too old.
 */
object CastProtocol {
    /**
     * Protocol version this sender speaks.
     *
     * v2: payload urls (contentUrl / subtitle MediaTracks) stay RAW; customData and the handshake
     * gain `proxyBaseUrl`; receivers may reply on the handshake channel with a
     * ReceiverHandshakeMessage (only the web receiver does). All additive — Terebi
     * (MIN_SENDER_VERSION = 1) is unaffected.
     *
     * v3: the phone proxy is a STATELESS forwarder — `GET /proxy?url=<enc>&headers=<enc-json>`,
     * with the source headers carried by the receiver on each request (no phone-side media
     * registry, no m3u8 rewriting); the web receiver proxies only header-bound sources and fixes
     * broken relative-URI resolution itself by detecting the proxy origin in request URLs. Still
     * additive for Terebi, which never touches the proxy.
     */
    const val SENDER_VERSION = 3

    /**
     * Oldest receiver protocol this sender can drive correctly. Receivers below this must update.
     * Stays at 2: this gates the native Terebi receiver too (protocol 2, proxy-agnostic — v3
     * changes nothing it uses), and no protocol-2 WEB receiver was ever deployed, so the only
     * pairing v3 breaks cannot occur in the wild.
     */
    const val MIN_RECEIVER_VERSION = 2
}
