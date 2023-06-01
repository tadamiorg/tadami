package com.sf.tadami.ui.animeinfos.episode.cast.channels

import com.google.android.gms.cast.Cast

abstract class CustomCastChannel : Cast.MessageReceivedCallback{
    abstract val namespace: String
}