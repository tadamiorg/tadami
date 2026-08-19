package com.sf.tadami.extension.model

/**
 * Whether the initial extension load pass has finished. Because loading now happens off the main
 * thread, an empty source list no longer means "no extensions" — it may just mean "not yet".
 */
sealed interface ExtensionsLoadState {
    data object Loading : ExtensionsLoadState
    data object Ready : ExtensionsLoadState
}
