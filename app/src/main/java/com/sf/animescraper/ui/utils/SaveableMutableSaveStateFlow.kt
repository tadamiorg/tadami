package com.sf.animescraper.ui.utils

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SaveableMutableSaveStateFlow<T>(
    private val savedStateHandle: SavedStateHandle,
    private val key: String,
    defaultValue: T
) {
    private val _state: MutableStateFlow<T> =
        MutableStateFlow(
            savedStateHandle.get<T>(key) ?: defaultValue)

    var value: T
        get() = _state.value
        set(value) {
            _state.update {
                savedStateHandle[key] = value
                value
            }
        }
    fun asStateFlow(): StateFlow<T> = _state.asStateFlow()
}