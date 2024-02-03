package com.sf.tadami.ui.tabs.browse.tabs.extensions

import androidx.annotation.StringRes
import com.sf.tadami.domain.extensions.Extension
import com.sf.tadami.extensions.model.InstallStep

object ExtensionUiModel {
    sealed interface Header {
        data class Resource(@StringRes val textRes: Int) : Header
        data class Text(@StringRes val textRes: Int) : Header
    }

    data class Item(
        val extension: Extension,
        val installStep: InstallStep,
    )
}