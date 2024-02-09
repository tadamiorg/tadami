package com.sf.tadami.data.interactors.extension

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.sf.tadami.extension.ExtensionManager
import com.sf.tadami.preferences.sources.SourcesPreferences
import com.sf.tadami.utils.Lang
import com.sf.tadami.utils.getPreferencesGroupAsFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GetExtensionLanguages(
    private val context: Context,
    private val dataStore: DataStore<Preferences>,
    private val extensionManager: ExtensionManager,
) {
    fun subscribe(): Flow<List<Lang>> {
        return combine(
            dataStore.getPreferencesGroupAsFlow(SourcesPreferences),
            extensionManager.availableExtensionsFlow,
        ) { sourcesPreferences, availableExtensions ->
            val enabledLanguages = sourcesPreferences.enabledLanguages
            availableExtensions
                .flatMap { ext ->
                    if (ext.sources.isEmpty()) {
                        listOf(ext.lang)
                    } else {
                        ext.sources.map { it.lang }
                    }
                }
                .distinct()
                .sortedWith(
                    compareBy<Lang> { it.name !in enabledLanguages }.then { a, b ->
                        context.getString(a.getRes()).compareTo(context.getString(b.getRes()))
                    },
                )
        }
    }
}