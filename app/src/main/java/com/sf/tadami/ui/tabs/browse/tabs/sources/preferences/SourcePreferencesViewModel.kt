package com.sf.tadami.ui.tabs.browse.tabs.sources.preferences

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.sf.tadami.source.online.ConfigurableParsedHttpAnimeSource
import com.sf.tadami.ui.tabs.browse.SourceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SourcePreferencesViewModel(
    stateHandle: SavedStateHandle
) : ViewModel() {
    private val sourcesManager: SourceManager = Injekt.get()
    private val sourceId: Long = checkNotNull(stateHandle["sourceId"])

    private val source = sourcesManager.getOrStub(sourceId) as ConfigurableParsedHttpAnimeSource<*>
    private var _uiState = MutableStateFlow(SourcePreferencesUiState(source.name,source.getPreferenceScreen().preferences))
    val uiState = _uiState.asStateFlow()

    val dataStore = source.dataStore
}