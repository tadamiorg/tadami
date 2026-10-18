package com.sf.tadami.source.online

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.sf.tadami.preferences.model.CustomPreferences
import com.sf.tadami.preferences.model.CustomPreferencesIdentifier
import com.sf.tadami.source.ConfigurableSource
import com.sf.tadami.ui.tabs.browse.tabs.sources.preferences.SourcesPreferencesContent
import kotlinx.coroutines.flow.StateFlow

/**
 * An [AnimeHttpSource] with preferences, like [ConfigurableParsedHttpAnimeSource] without the Jsoup
 * selectors: for a source that parses JSON, or implements the suspend methods itself.
 *
 * @param id the source ID, `BuildConfig.SOURCE_ID` in tadami-extensions-source
 * @param prefGroup reads and writes its preferences, of type [T]
 * @since extensions-lib 1.7
 */
abstract class ConfigurableAnimeHttpSource<T : CustomPreferencesIdentifier>(
    final override val id: Long,
    prefGroup: CustomPreferences<T>,
) : AnimeHttpSource(), ConfigurableSource {

    /**
     * Where its preferences are stored, the file `anime_source_<id>`.
     *
     * @since extensions-lib 1.7
     */
    final override val dataStore: DataStore<Preferences>
        get() = throw Exception("Stub!")

    /**
     * Its preferences, updated when the user edits them.
     *
     * @since extensions-lib 1.7
     */
    val preferencesState: StateFlow<T> = throw Exception("Stub!")

    /**
     * Its preferences now, see [preferencesState]. Already read in the init blocks of the subclasses.
     *
     * @since extensions-lib 1.7
     */
    val preferences: T = throw Exception("Stub!")

    /**
     * The settings screen of the source: its title and its preferences.
     *
     * @since extensions-lib 1.7
     */
    abstract override fun getPreferenceScreen(): SourcesPreferencesContent
}
