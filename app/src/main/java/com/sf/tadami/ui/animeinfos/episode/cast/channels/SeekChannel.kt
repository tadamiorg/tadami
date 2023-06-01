package com.sf.tadami.ui.animeinfos.episode.cast.channels

import android.util.Log
import com.google.android.gms.cast.CastDevice

class SeekChannel : CustomCastChannel() {
    override val namespace: String
        get() = NAMESPACE

    override fun onMessageReceived(castDevice: CastDevice, namespace: String, message: String) {
        Log.d("CustomSeek", "onMessageReceived: $message")
    }
    companion object{
        const val NAMESPACE : String = "urn:x-cast:custom.seek"
    }
}