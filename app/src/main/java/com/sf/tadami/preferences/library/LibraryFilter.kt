package com.sf.tadami.preferences.library

import androidx.compose.ui.state.ToggleableState

data class LibraryFilter(
    val flags : Long
) {
    val readState : ToggleableState
        get() =  when(flags and READ_MASK){
            UNREAD -> ToggleableState.On
            READ -> ToggleableState.Off
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

        const val READ = 0x00000001L
        const val UNREAD = 0x00000002L
        const val READ_MASK = 0x00000003L

        const val STARTED = 0x00000004L
        const val UNSTARTED = 0x00000008L
        const val STARTED_MASK = 0x0000000CL
    }
}