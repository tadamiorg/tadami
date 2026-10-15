package com.sf.tadami.ui.tabs.browse.tabs.extensions.details

import androidx.compose.runtime.Immutable
import com.sf.tadami.domain.extensions.Extension

@Immutable
data class ExtensionDetailsUiState(
    val extension: Extension.Installed? = null,
    /** Whether it's being loaded again, once trusted for instance. */
    val isReloading: Boolean = false,
) {
    val isLoading: Boolean
        get() = extension == null
}