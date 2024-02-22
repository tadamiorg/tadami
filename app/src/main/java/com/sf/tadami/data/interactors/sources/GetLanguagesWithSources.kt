package com.sf.tadami.data.interactors.sources

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.sf.tadami.data.sources.SourceRepository
import com.sf.tadami.domain.source.Source
import com.sf.tadami.preferences.sources.SourcesPreferences
import com.sf.tadami.utils.Lang
import com.sf.tadami.utils.getPreferencesGroupAsFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.SortedMap

class GetLanguagesWithSources(
    private val context: Context,
    private val repository: SourceRepository,
    private val dataStore: DataStore<Preferences>,
) {

    fun subscribe(): Flow<SortedMap<Lang, List<Source>>> {
        return combine(
            dataStore.getPreferencesGroupAsFlow(SourcesPreferences),
            repository.getOnlineSources(),
        ) { sourcesPreferences, onlineSources ->
            val enabledLanguages = sourcesPreferences.enabledLanguages
            val hiddenSources = sourcesPreferences.hiddenSources
            val sortedSources = onlineSources.sortedWith(
                compareBy<Source> { it.id.toString() in hiddenSources }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name },
            )

            sortedSources
                .groupBy { it.lang }
                .toSortedMap(
                    compareBy<Lang> { it.name !in enabledLanguages }
                        .then { a, b ->
                            context.getString(a.getRes()).compareTo(context.getString(b.getRes()))
                        },
                )
        }
    }
}