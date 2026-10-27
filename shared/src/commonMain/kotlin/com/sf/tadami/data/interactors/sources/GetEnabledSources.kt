package com.sf.tadami.data.interactors.sources

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.sf.tadami.data.sources.SourceRepository
import com.sf.tadami.domain.source.Source
import com.sf.tadami.preferences.sources.SourcesPreferences
import com.sf.tadami.utils.getPreferencesGroupAsFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

class GetEnabledSources(
    private val repository: SourceRepository,
    private val dataStore: DataStore<Preferences>,
) {
    fun subscribe(): Flow<List<Source>> {
        return combine(
            dataStore.getPreferencesGroupAsFlow(SourcesPreferences),
            repository.getSources(),
        ) { preferences, sources ->
            val enabledLanguages = preferences.enabledLanguages
            val hiddenSources = preferences.hiddenSources
            sources
                .filter { it.lang.name in enabledLanguages }
                .filterNot { it.id.toString() in hiddenSources }
                .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
                .flatMap {
                    mutableListOf(it)
                }
        }.distinctUntilChanged()
    }
}