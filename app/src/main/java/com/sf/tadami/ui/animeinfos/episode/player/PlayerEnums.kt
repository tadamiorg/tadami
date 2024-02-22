package com.sf.tadami.ui.animeinfos.episode.player

enum class PipState {
    OFF, ON, STARTED;

    companion object {
        internal var mode: PipState = OFF
    }
}