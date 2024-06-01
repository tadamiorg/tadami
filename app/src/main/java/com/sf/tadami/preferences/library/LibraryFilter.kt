package com.sf.tadami.preferences.library

import androidx.compose.ui.state.ToggleableState

data class LibraryFilter(
    val flags : Long
) {
    val seenState : ToggleableState
        get() =  when(flags and SEEN_MASK){
            UNSEEN -> ToggleableState.On
            SEEN -> ToggleableState.Off
            else -> ToggleableState.Indeterminate
        }

    val startedState  : ToggleableState
        get() =  when(flags and STARTED_MASK){
            STARTED -> ToggleableState.On
            UNSTARTED -> ToggleableState.Off
            else -> ToggleableState.Indeterminate
        }

    val isFiltered : Boolean
        get() = flags != 0L
    companion object {
        const val DEFAULT_FILTER = 0x00000000L

        const val SEEN = 0x00000001L
        const val UNSEEN = 0x00000002L
        const val SEEN_MASK = 0x00000003L

        const val STARTED = 0x00000004L
        const val UNSTARTED = 0x00000008L
        const val STARTED_MASK = 0x0000000CL
    }
}