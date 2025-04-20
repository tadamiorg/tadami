package com.sf.tadami.ui.animeinfos.episode.player.controls.dialogs.settings.tabs

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.CaptionStyleCompat
import com.sf.tadami.R
import com.sf.tadami.preferences.model.DataStoreState
import com.sf.tadami.preferences.model.Preference
import com.sf.tadami.preferences.model.rememberDataStoreState
import com.sf.tadami.preferences.player.PlayerPreferences
import com.sf.tadami.ui.components.screens.ScreenTabContent
import com.sf.tadami.ui.tabs.more.settings.components.PreferenceParser


@OptIn(UnstableApi::class)
fun PlayerPreferences.toSubtitlesStyle(): CaptionStyleCompat {
    return CaptionStyleCompat(
        this.subtitleTextColor,
        this.subtitleBackgroundColor,
        Color.Transparent.toArgb(), // Edge color handled separately
        this.subtitleEdgeType,
        this.subtitleEdgeColor,
        null // We'll set the typeface through PlayerView
    )
}

@OptIn(UnstableApi::class)
@Composable()
fun applicationTab(): ScreenTabContent {
    val playerPreferencesState = rememberDataStoreState(PlayerPreferences)
    val playerPreferences by playerPreferencesState.value.collectAsState()
    return ScreenTabContent(
        titleRes = R.string.notification_app_group,
    ) { contentPadding: PaddingValues, _ ->
        PreferenceParser(
            customPrefsVerticalPadding = 8.dp,
            modifier = Modifier.padding(contentPadding),
            items = listOf(
                getTimelineGroup(prefState = playerPreferencesState, prefs = playerPreferences),
                getSubtitlesGroup(prefState = playerPreferencesState, prefs = playerPreferences)
            )
        )
    }
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