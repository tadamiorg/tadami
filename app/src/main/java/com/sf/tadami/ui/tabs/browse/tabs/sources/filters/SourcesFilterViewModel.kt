package com.sf.tadami.ui.tabs.browse.tabs.sources.filters

import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sf.tadami.data.interactors.sources.GetLanguagesWithSources
import com.sf.tadami.domain.source.Source
import com.sf.tadami.preferences.sources.SourcesPreferences
import com.sf.tadami.utils.editPreferences
import com.sf.tadami.utils.getPreferencesGroup
import com.sf.tadami.utils.getPreferencesGroupAsFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.SortedMap

class SourcesFilterViewModel(
    private val dataStore: DataStore<Preferences> = Injekt.get(),
    private val getLanguagesWithSources: GetLanguagesWithSources = Injekt.get(),
) : ViewModel() {

    private val _uiState : MutableStateFlow<SourcesFilterUiState> = MutableStateFlow(
        SourcesFilterUiState.Loading
    )
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                getLanguagesWithSources.subscribe(),
                dataStore.getPreferencesGroupAsFlow(SourcesPreferences)
            ) { a, b -> Triple(a, b.enabledLanguages, b.hiddenSources) }
                .catch { throwable ->
                    _uiState.update {
                        SourcesFilterUiState.Error(
                            throwable = throwable,
                        )
                    }
                }
                .collectLatest { (languagesWithSources, enabledLanguages, hiddenSources) ->
                    _uiState.update {
                        SourcesFilterUiState.Success(
                            items = languagesWithSources,
                            enabledLanguages = enabledLanguages,
                            hiddenSources = hiddenSources,
                        )
                    }
                }
        }
    }

    fun toggleSource(source: Source) {
        viewModelScope.launch {
            val sourceId = source.id.toString()
            val sourcesPreferences = dataStore.getPreferencesGroup(SourcesPreferences)
            val isEnabled = sourceId in sourcesPreferences.hiddenSources
            dataStore.editPreferences(
                sourcesPreferences.let { old ->
                    old.copy(
                        hiddenSources = if (isEnabled) old.hiddenSources.minus(sourceId) else old.hiddenSources.plus(
                            sourceId
                        )
                    )
                }, SourcesPreferences
            )
        }

    }

    fun toggleLanguage(language: String) {
        viewModelScope.launch {
            val sourcesPreferences = dataStore.getPreferencesGroup(SourcesPreferences)
            val isEnabled = language in sourcesPreferences.enabledLanguages
            dataStore.editPreferences(
                sourcesPreferences.let { old ->
                    old.copy(
                        enabledLanguages = if (isEnabled) old.enabledLanguages.minus(language) else old.enabledLanguages.plus(
                            language
                        )
                    )
                }, SourcesPreferences
            )
        }
    }

    sealed interface State {

        @Immutable
        data object Loading : State

        @Immutable
        data class Error(
            val throwable: Throwable,
        ) : State

        @Immutable
        data class Success(
            val items: SortedMap<String, List<Source>>,
            val enabledLanguages: Set<String>,
            val disabledSources: Set<String>,
        ) : State {

            val isEmpty: Boolean
                get() = items.isEmpty()
        }
    }
}