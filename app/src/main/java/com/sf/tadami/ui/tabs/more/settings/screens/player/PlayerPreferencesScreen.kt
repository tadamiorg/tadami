package com.sf.tadami.ui.tabs.more.settings.screens.player

import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavHostController
import com.sf.tadami.R
import com.sf.tadami.preferences.model.DataStoreState
import com.sf.tadami.preferences.model.Preference
import com.sf.tadami.preferences.model.rememberDataStoreState
import com.sf.tadami.preferences.player.PlayerPreferences
import com.sf.tadami.ui.tabs.more.settings.components.PreferenceScreen

class PlayerPreferencesScreen(
    navController: NavHostController
) : PreferenceScreen {
    override val title: Int = R.string.preferences_player_title

    override val backHandler: (() -> Unit) = { navController.navigateUp() }

    @OptIn(UnstableApi::class)
    @Composable
    override fun getPreferences(): List<Preference> {
        val playerPreferencesState = rememberDataStoreState(PlayerPreferences)
        val playerPreferences by playerPreferencesState.value.collectAsState()
        return listOf(
            getTimelineGroup(prefState = playerPreferencesState, prefs = playerPreferences),
            getSubtitlesGroup(prefState = playerPreferencesState, prefs = playerPreferences),
            getDisplayGroup(prefState = playerPreferencesState, prefs = playerPreferences)
        )
    }

    @OptIn(UnstableApi::class)
    @Composable
    private fun getTimelineGroup(
        prefState: DataStoreState<PlayerPreferences>,
        prefs: PlayerPreferences
    ): Preference.PreferenceCategory {
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
                        prefState.setValue(
                            prefs.copy(
                                seenThreshold = it
                            )
                        )
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
                    ).associateWith { "${it / 1000}s" },
                    title = stringResource(id = R.string.preferences_player_double_tap_length),
                    onValueChanged = {
                        prefState.setValue(
                            prefs.copy(
                                doubleTapLength = it
                            )
                        )
                        true
                    }
                ),
                Preference.PreferenceItem.TogglePreference(
                    title = stringResource(id = R.string.player_pref_auto_play),
                    value = prefs.autoPlay,
                    subtitle = stringResource(id = R.string.player_pref_auto_play_subtitle),
                    onValueChanged = {
                        prefState.setValue(
                            prefs.copy(
                                autoPlay = it
                            )
                        )
                        true
                    }
                )
            )
        )
    }

    @OptIn(UnstableApi::class)
    @Composable
    private fun getSubtitlesGroup(
        prefState: DataStoreState<PlayerPreferences>,
        prefs: PlayerPreferences
    ): Preference.PreferenceCategory {
        return Preference.PreferenceCategory(
            title = stringResource(id = R.string.label_subtitles),
            preferenceItems = listOf(
                Preference.PreferenceItem.TogglePreference(
                    title = stringResource(id = R.string.enable_subtitles_label),
                    value = prefs.subtitlesEnabled,
                    subtitle = stringResource(id = R.string.enable_subtitles_description),
                    onValueChanged = {
                        prefState.setValue(
                            prefs.copy(
                                subtitlesEnabled = it
                            )
                        )
                        true
                    }
                ),
                Preference.PreferenceItem.ReorderStringPreference(
                    enabled = prefs.subtitlesEnabled,
                    value = prefs.subtitlePrefLanguages,
                    items = PlayerPreferences.DefaultSubtitlesPrefLanguages.mapValues { (key, value) ->
                        stringResource(value)
                    },
                    title = stringResource(id = R.string.subtitles_priority_label),
                    subtitle = stringResource(id = R.string.subtitles_priority_description),
                    onValueChanged = {
                        prefState.setValue(
                            prefs.copy(
                                subtitlePrefLanguages = it
                            )
                        )
                        true
                    }
                ),
            )
        )
    }

    @OptIn(UnstableApi::class)
    @Composable
    private fun getDisplayGroup(
        prefState: DataStoreState<PlayerPreferences>,
        prefs: PlayerPreferences
    ): Preference.PreferenceCategory {
        return Preference.PreferenceCategory(
            title = stringResource(id = R.string.title_display),
            preferenceItems = listOf(
                Preference.PreferenceItem.TogglePreference(
                    title = stringResource(id = R.string.display_cutout_pref_title),
                    value = prefs.ignoreCutout,
                    subtitle = stringResource(id = R.string.display_cutout_pref_subtitle),
                    onValueChanged = {
                        prefState.setValue(
                            prefs.copy(
                                ignoreCutout = it
                            )
                        )
                        true
                    }
                ),
            )
        )
    }
}