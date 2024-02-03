package com.sf.tadami.data.interactors.extension

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.sf.tadami.domain.extensions.Extension
import com.sf.tadami.domain.extensions.Extensions
import com.sf.tadami.extensions.ExtensionManager
import com.sf.tadami.preferences.sources.SourcesPreferences
import com.sf.tadami.utils.getPreferencesGroupAsFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GetExtensionsByType(
    private val dataStore: DataStore<Preferences>,
    private val extensionManager: ExtensionManager,
) {

    fun subscribe(): Flow<Extensions> {

        return combine(
            dataStore.getPreferencesGroupAsFlow(SourcesPreferences),
            extensionManager.installedExtensionsFlow,
            extensionManager.availableExtensionsFlow,
        ) { _sourcesPreferences, _installed, _available ->
            val _enabledLanguages = _sourcesPreferences.enabledLanguages
            val (updates, installed) = _installed
                .sortedWith(
                    compareBy<Extension.Installed> { it.isObsolete.not() }
                        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name },
                )
                .partition { it.hasUpdate }
            val available = _available
                .filter { extension ->
                    _installed.none { it.pkgName == extension.pkgName }
                }
                .flatMap { ext ->
                    if (ext.sources.isEmpty()) {
                        return@flatMap if (ext.lang.name in _enabledLanguages) listOf(ext) else emptyList()
                    }
                    ext.sources.filter { it.lang.name in _enabledLanguages }
                        .map {
                            ext.copy(
                                name = it.name,
                                lang = it.lang,
                                pkgName = "${ext.pkgName}-${it.id}",
                                sources = listOf(it),
                            )
                        }
                }
                .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })

            Extensions(updates, installed, available)
        }
    }
}