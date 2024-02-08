package com.sf.tadami.ui.tabs.browse.tabs.extensions.details

import androidx.compose.runtime.Immutable
import com.sf.tadami.domain.extensions.Extension

@Immutable
data class ExtensionDetailsUiState(
    val extension: Extension.Installed? = null,
) {
    val isLoading: Boolean
        get() = extension == null
}