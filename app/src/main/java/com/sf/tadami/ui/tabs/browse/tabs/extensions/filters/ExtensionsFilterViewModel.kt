package com.sf.tadami.ui.tabs.browse.tabs.extensions.filters

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sf.tadami.data.interactors.extension.GetExtensionLanguages
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

class ExtensionsFilterViewModel : ViewModel(){
    private val dataStore: DataStore<Preferences> = Injekt.get()
    private val getExtensionLanguages: GetExtensionLanguages = Injekt.get()

    private val _uiState : MutableStateFlow<ExtensionsFilterUiState> = MutableStateFlow(
        ExtensionsFilterUiState.Loading
    )
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                getExtensionLanguages.subscribe(),
                dataStore.getPreferencesGroupAsFlow(SourcesPreferences),
            ) { a, b -> a to b.enabledLanguages }
                .catch { throwable ->
                    Log.d("ExtensionsFilterViewModelInit", throwable.stackTraceToString())
                }
                .collectLatest { (extensionLanguages, enabledLanguages) ->
                    _uiState.update {
                        ExtensionsFilterUiState.Success(
                            languages = extensionLanguages,
                            enabledLanguages = enabledLanguages,
                        )
                    }
                }
        }
    }
    fun toggle(language: String) {
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
}