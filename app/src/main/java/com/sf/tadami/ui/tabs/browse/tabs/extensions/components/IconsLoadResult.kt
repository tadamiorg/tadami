package com.sf.tadami.ui.tabs.browse.tabs.extensions.components

sealed class IconsLoadResult<out T> {
    data object Loading : IconsLoadResult<Nothing>()
    data object Error : IconsLoadResult<Nothing>()
    data class Success<out T>(val value: T) : IconsLoadResult<T>()
}