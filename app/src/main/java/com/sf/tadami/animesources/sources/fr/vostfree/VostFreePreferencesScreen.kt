package com.sf.tadami.animesources.sources.fr.vostfree

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.navigation.NavHostController
import com.sf.tadami.R
import com.sf.tadami.ui.components.data.Action
import com.sf.tadami.ui.tabs.settings.components.PreferenceScreen
import com.sf.tadami.ui.tabs.settings.model.DataStoreState
import com.sf.tadami.ui.tabs.settings.model.Preference
import com.sf.tadami.ui.tabs.settings.model.rememberDataStoreState

class VostFreePreferencesScreen(
    navController: NavHostController,
    dataStore: DataStore<Preferences>
) : PreferenceScreen {

    override val title: Int = R.string.sources_preferences_title

    @Composable
    override fun getTitle(): String {
        return stringResource(title,"VostFree")
    }

    override val backHandler: (() -> Unit) = {
        navController.navigateUp()
    }

    override val getCustomDataStore: (() -> DataStore<Preferences>) = {
        dataStore
    }

    override val topBarActions: List<Action> = listOf()

    @Composable
    override fun getPreferences(): List<Preference> {
        val vostFreePreferencesState = rememberDataStoreState(VostFreePreferences,getCustomDataStore())
        val gogoAnimePreferences by vostFreePreferencesState.value.collectAsState()

        return listOf(
            getNetworkGroup(prefState = vostFreePreferencesState, prefs = gogoAnimePreferences)
        )
    }

    @Composable
    fun getNetworkGroup(
        prefState: DataStoreState<VostFreePreferences>,
        prefs: VostFreePreferences
    ): Preference.PreferenceCategory {
        return Preference.PreferenceCategory(
            title = stringResource(id = R.string.category_network),
            preferenceItems = listOf(
                Preference.PreferenceItem.EditTextPreference(
                    value = prefs.baseUrl,
                    title = stringResource(id = R.string.sources_preferences_base_url),
                    subtitle = stringResource(id = R.string.sources_preferences_base_url_subtitle),
                    defaultValue = VostFreePreferences.DEFAULT_BASE_URL,
                    onValueChanged = {
                        prefState.setValue(
                            prefs.copy(
                                baseUrl = it
                            )
                        )
                        true
                    }
                )
            )
        )
    }
}