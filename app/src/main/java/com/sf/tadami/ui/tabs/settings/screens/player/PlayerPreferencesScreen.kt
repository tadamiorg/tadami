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
                    items = listOf(
                       PlayerPreferences.SeenThresholdItems.SEVENTY,
                        PlayerPreferences.SeenThresholdItems.SEVENTY_FIVE,
                        PlayerPreferences.SeenThresholdItems.EIGHTY,
                        PlayerPreferences.SeenThresholdItems.EIGHTY_FIVE,
                        PlayerPreferences.SeenThresholdItems.NINETY,
                        PlayerPreferences.SeenThresholdItems.NINETY_FIVE,
                        PlayerPreferences.SeenThresholdItems.HUNDRED,
                    ).associateWith { "$it%" },

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
                    items = listOf(
                        PlayerPreferences.DoubleTapLengthItems.FIVE,
                        PlayerPreferences.DoubleTapLengthItems.TEN,
                        PlayerPreferences.DoubleTapLengthItems.FIFTEEN,
                        PlayerPreferences.DoubleTapLengthItems.TWENTY,
                        PlayerPreferences.DoubleTapLengthItems.TWENTY_FIVE,
                        PlayerPreferences.DoubleTapLengthItems.THIRTY,
                    ).associateWith { "${it/1000}s" },
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