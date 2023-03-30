package com.sf.tadami.ui.tabs.settings.screens.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import com.sf.tadami.R
import com.sf.tadami.ui.components.data.Action
import com.sf.tadami.ui.tabs.settings.components.PreferenceScreen
import com.sf.tadami.ui.tabs.settings.model.DataStoreState
import com.sf.tadami.ui.tabs.settings.model.Preference
import com.sf.tadami.ui.tabs.settings.model.rememberDataStoreState

class PlayerPreferencesScreen(
    navController : NavHostController
) : PreferenceScreen {
    override val title: Int = R.string.preferences_player_title

    override val backHandler: (() -> Unit) = { navController.navigateUp() }

    override val topBarActions: List<Action> = emptyList()

    @Composable
    override fun getPreferences(): List<Preference> {
        val playerPreferencesState = rememberDataStoreState(PlayerPreferences)
        val playerPreferences by playerPreferencesState.value.collectAsState()
        return listOf(
            getTimelineGroup(prefState = playerPreferencesState, prefs = playerPreferences)
        )
    }

    @Composable
    private fun getTimelineGroup(
        prefState: DataStoreState<PlayerPreferences>,
        prefs: PlayerPreferences
    ) : Preference.PreferenceCategory{
        return Preference.PreferenceCategory(
            title = stringResource(id = R.string.preferences_player_timeline),
            preferenceItems = listOf(
                Preference.PreferenceItem.SelectPreference(
                    value = prefs.seenThreshold,
                    items = mapOf(
                        70 to "70%",
                        75 to "75%",
                        80 to "80%",
                        85 to "85%",
                        90 to "90%",
                        95 to "95%",
                        100 to "100%"
                    ),
                    title = stringResource(id = R.string.preferences_player_seenthreshold),
                    onValueChanged = {
                        prefState.setValue(prefs.copy(
                            seenThreshold = it
                        ))
                        true
                    }
                ),
                Preference.PreferenceItem.SelectPreference(
                    value = prefs.doubleTapLength,
                    items = mapOf(
                        5000L to "5s",
                        10000L to "10s",
                        15000L to "15s",
                        20000L to "20s",
                        25000L to "25s",
                        30000L to "30s",
                    ),
                    title = stringResource(id = R.string.preferences_player_double_tap_length),
                    onValueChanged = {
                        prefState.setValue(prefs.copy(
                            doubleTapLength = it
                        ))
                        true
                    }
                )
            )
        )
    }
}