package com.sf.tadami.data.interactors.extension

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.sf.tadami.extension.ExtensionManager
import com.sf.tadami.preferences.sources.SourcesPreferences
import com.sf.tadami.utils.getPreferencesGroupAsFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GetExtensionLanguages(
    private val dataStore: DataStore<Preferences>,
    private val extensionManager: ExtensionManager,
) {
    fun subscribe(): Flow<List<String>> {
        return combine(
            dataStore.getPreferencesGroupAsFlow(SourcesPreferences),
            extensionManager.availableExtensionsFlow,
        ) { sourcesPreferences, availableExtensions ->
            val enabledLanguages = sourcesPreferences.enabledLanguages
            availableExtensions
                .flatMap { ext ->
                    if (ext.sources.isEmpty()) {
                        listOf(ext.lang.name)
                    } else {
                        ext.sources.map { it.lang.name }
                    }
                }
                .distinct()
                .sortedWith(
                    compareBy<String> { it !in enabledLanguages },
                )
        }
    }
}