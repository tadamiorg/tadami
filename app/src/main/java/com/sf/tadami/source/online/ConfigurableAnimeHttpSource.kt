package com.sf.tadami.source.online

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.sf.tadami.preferences.model.CustomPreferences
import com.sf.tadami.preferences.model.CustomPreferencesIdentifier
import com.sf.tadami.source.ConfigurableSource
import com.sf.tadami.ui.tabs.browse.tabs.sources.preferences.SourcesPreferencesContent
import com.sf.tadami.utils.CollectAsStateValue
import kotlinx.coroutines.flow.StateFlow

/**
 * An [AnimeHttpSource] with preferences, like [ConfigurableParsedHttpAnimeSource] without the Jsoup
 * selectors: for a source that parses JSON, or implements the suspend methods itself.
 *
 * @since extensions-lib 1.7
 */
abstract class ConfigurableAnimeHttpSource<T : CustomPreferencesIdentifier>(
    final override val id: Long,
    prefGroup: CustomPreferences<T>,
) : AnimeHttpSource(), ConfigurableSource {

    private val sourcePreferences = SourcePreferences(id, SourcePreferences.fileName(id), prefGroup)

    final override val dataStore: DataStore<Preferences>
        get() = sourcePreferences.dataStore

    /** Its preferences, updated when the user edits them. */
    val preferencesState: StateFlow<T> = sourcePreferences.state

    /** Its preferences now, see [preferencesState]. */
    val preferences by CollectAsStateValue(preferencesState)

    /** Stops listening to the preferences changes, see [ConfigurableParsedHttpAnimeSource.cancelPreferencesUpdates]. */
    internal fun cancelPreferencesUpdates() {
        sourcePreferences.cancelUpdates()
    }

    abstract override fun getPreferenceScreen(): SourcesPreferencesContent
}
