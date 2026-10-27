package com.sf.tadami.ui.tabs.browse.tabs.extensions

import org.jetbrains.compose.resources.StringResource
import com.sf.tadami.domain.extensions.Extension
import com.sf.tadami.extension.model.InstallStep

object ExtensionUiModel {
    sealed interface Header {
        data class Resource(val textRes: StringResource) : Header
        data class Text(val textRes: StringResource) : Header
    }

    data class Item(
        val extension: Extension,
        val installStep: InstallStep,
    )
}