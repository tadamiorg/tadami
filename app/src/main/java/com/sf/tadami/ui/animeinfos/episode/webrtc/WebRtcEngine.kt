package com.sf.tadami.ui.animeinfos.episode.webrtc

import android.content.Context
import org.webrtc.PeerConnectionFactory

/** Process-wide WebRTC factory (data-channel only). */
object WebRtcEngine {

    @Volatile
    private var initialized = false

    lateinit var factory: PeerConnectionFactory
        private set

    @Synchronized
    fun ensureInitialized(context: Context) {
        if (initialized) return
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions
                .builder(context.applicationContext)
                .createInitializationOptions(),
        )
        factory = PeerConnectionFactory.builder()
            .setOptions(PeerConnectionFactory.Options())
            .createPeerConnectionFactory()
        initialized = true
    }
}
