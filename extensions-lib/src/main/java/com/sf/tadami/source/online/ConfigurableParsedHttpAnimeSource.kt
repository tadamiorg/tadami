package com.sf.tadami.source.online

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.sf.tadami.preferences.model.CustomPreferences
import com.sf.tadami.preferences.model.CustomPreferencesIdentifier
import com.sf.tadami.source.ConfigurableSource
import com.sf.tadami.ui.tabs.browse.tabs.sources.preferences.SourcesPreferencesContent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * A [ParsedAnimeHttpSource] with preferences, stored in its [dataStore]: the user edits them in the
 * screen [getPreferenceScreen] describes.
 *
 * @param sourceId the source ID, see [com.sf.tadami.source.Source.id]
 * @param prefGroup reads and writes its preferences, of type [T]
 * @since extensions-lib 1.5
 */
abstract class ConfigurableParsedHttpAnimeSource<T : CustomPreferencesIdentifier>(sourceId : Long,prefGroup: CustomPreferences<T>) :
    ParsedAnimeHttpSource(sourceId), ConfigurableSource {

    // Preferences
    private val PREFERENCES_FILE_NAME : String = throw Exception("Stub!")

    /**
     * Where its preferences are stored, the file `anime_source_<id>`.
     *
     * @since extensions-lib 1.5
     */
    final override val dataStore: DataStore<Preferences>
        get() = throw Exception("Stub!")

    private val _preferences: MutableStateFlow<T> = throw Exception("Stub!")

    /**
     * Its preferences, updated when the user edits them.
     *
     * @since extensions-lib 1.5
     */
    val preferencesState: StateFlow<T> = throw Exception("Stub!")

    /**
     * Its preferences now, see [preferencesState]. Already read in the init blocks of the subclasses.
     *
     * @since extensions-lib 1.5
     */
    val preferences : T = throw Exception("Stub!")

    /**
     * The settings screen of the source: its title and its preferences.
     *
     * @since extensions-lib 1.5
     */
    abstract override fun getPreferenceScreen(): SourcesPreferencesContent
}
