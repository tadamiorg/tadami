package com.sf.tadami.ui.tabs.browse.tabs.sources

import androidx.annotation.StringRes
import com.sf.tadami.domain.source.Source

sealed interface SourcesUiModel {
    data class Item(val source: Source) : SourcesUiModel
    data class Header(@StringRes val language: Int) : SourcesUiModel
}
