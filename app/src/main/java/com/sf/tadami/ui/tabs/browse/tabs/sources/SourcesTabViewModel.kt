package com.sf.tadami.ui.tabs.browse.tabs.sources

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sf.tadami.data.interactors.sources.GetEnabledSources
import com.sf.tadami.domain.extensions.Extension
import com.sf.tadami.domain.source.Source
import com.sf.tadami.extension.ExtensionManager
import com.sf.tadami.preferences.sources.SourcesPreferences
import com.sf.tadami.utils.Lang
import com.sf.tadami.utils.getPreferencesGroupAsFlow
import com.sf.tadami.utils.launchIO
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.TreeMap

class SourcesTabViewModel : ViewModel() {
    private val getEnabledSources: GetEnabledSources = Injekt.get()
    private val extensionManager: ExtensionManager = Injekt.get()
    private val dataStore: DataStore<Preferences> = Injekt.get()

    private val _uiState = MutableStateFlow(SourcesUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launchIO {
            combine(getEnabledSources.subscribe(), unusableExtensions(), ::Pair)
                .catch {
                    Log.d("SourcesTabViewModelInit", it.stackTraceToString())
                }
                .collectLatest { (sources, unusableExtensions) ->
                    collectLatestSources(sources, unusableExtensions)
                }
        }
    }

    // Extensions that can't be used have no sources: they are listed in their place, disabled
    private fun unusableExtensions(): Flow<List<Extension.Installed>> {
        return combine(
            dataStore.getPreferencesGroupAsFlow(SourcesPreferences),
            extensionManager.installedExtensionsFlow,
        ) { preferences, extensions ->
            extensions.filter {
                it.loadIssue != null && (it.lang == Lang.UNKNOWN || it.lang.name in preferences.enabledLanguages)
            }
        }
    }

    private fun collectLatestSources(sources: List<Source>, unusableExtensions: List<Extension.Installed>) {
        _uiState.update { state ->
            val map = TreeMap<Lang, MutableList<SourcesUiModel>> { d1, d2 ->
                // Sources without a lang defined will be placed at the end
                when {
                    d1 == Lang.UNKNOWN && d2 != Lang.UNKNOWN -> 1
                    d2 == Lang.UNKNOWN && d1 != Lang.UNKNOWN -> -1
                    else -> d1.compareTo(d2)
                }
            }
            sources.forEach { map.getOrPut(it.lang) { mutableListOf() } += SourcesUiModel.Item(it) }
            unusableExtensions.forEach {
                map.getOrPut(it.lang) { mutableListOf() } += SourcesUiModel.UnusableExtension(it)
            }

            state.copy(
                isLoading = false,
                items = map
                    .flatMap { (lang, items) ->
                        listOf(SourcesUiModel.Header(lang.getRes())) +
                            items.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name() })
                    }
                    .toImmutableList(),
            )
        }
    }

    private fun SourcesUiModel.name(): String = when (this) {
        is SourcesUiModel.Item -> source.name
        is SourcesUiModel.UnusableExtension -> extension.name
        is SourcesUiModel.Header -> ""
    }
}