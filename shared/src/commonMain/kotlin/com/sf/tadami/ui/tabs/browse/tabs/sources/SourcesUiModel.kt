package com.sf.tadami.ui.tabs.browse.tabs.sources

import org.jetbrains.compose.resources.StringResource
import com.sf.tadami.domain.extensions.Extension
import com.sf.tadami.domain.source.Source

sealed interface SourcesUiModel {
    data class Item(val source: Source) : SourcesUiModel
    /** An installed extension that can't be used, shown disabled where its sources would be. */
    data class UnusableExtension(val extension: Extension.Installed) : SourcesUiModel
    data class Header(val language: StringResource) : SourcesUiModel
}
