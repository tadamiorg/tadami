package com.sf.tadami.ui.utils

import android.app.Application
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * App-wide channel for recoverable errors.
 *
 * Toasts vanish and can't be acted on, so failures that the user could retry are emitted here
 * instead and rendered as a snackbar with a Retry action by the host screen.
 */
object UiErrors {

    data class Event(
        val message: String,
        val retry: (() -> Unit)? = null,
    )

    // Replay 0, drop oldest: an error that nobody is listening for is stale by the time a screen
    // attaches, and we never want an emit to suspend the caller.
    private val _events = MutableSharedFlow<Event>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events = _events.asSharedFlow()

    fun emit(message: String, retry: (() -> Unit)? = null) {
        _events.tryEmit(Event(message, retry))
    }

    fun emit(throwable: Throwable, retry: (() -> Unit)? = null) {
        emit(with(Injekt.get<Application>()) { throwable.formattedMessage }, retry)
    }
}
