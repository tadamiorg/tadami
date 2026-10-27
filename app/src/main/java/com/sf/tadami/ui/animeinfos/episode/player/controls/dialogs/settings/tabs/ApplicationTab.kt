package com.sf.tadami.ui.animeinfos.episode.player.controls.dialogs.settings.tabs

import com.sf.tadami.R

import android.content.Context
import android.graphics.Typeface
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.CaptionStyleCompat
import com.sf.tadami.shared.resources.Res
import com.sf.tadami.shared.resources.*
import com.sf.tadami.preferences.model.DataStoreState
import com.sf.tadami.preferences.model.Preference
import com.sf.tadami.preferences.model.rememberDataStoreState
import com.sf.tadami.preferences.player.PlayerPreferences
import com.sf.tadami.ui.tabs.more.settings.components.PreferenceParser


@OptIn(UnstableApi::class)
fun PlayerPreferences.toSubtitlesStyle(context: Context): CaptionStyleCompat {
    return CaptionStyleCompat(
        this.subtitleTextColor,
        this.subtitleBackgroundColor,
        Color.Transparent.toArgb(), // Edge color handled separately
        this.subtitleEdgeType,
        this.subtitleEdgeColor,
        getCustomTypeface(context) // We'll set the typeface through PlayerView
    )
}

private fun getCustomTypeface(context: Context): Typeface {
    return ResourcesCompat.getFont(context, R.font.roboto_extrabold) ?: Typeface.DEFAULT
}

@OptIn(UnstableApi::class)
@Composable()
fun ApplicationTab() {
    val playerPreferencesState = rememberDataStoreState(PlayerPreferences)
    val playerPreferences by playerPreferencesState.value.collectAsState()
    PreferenceParser(
        customPrefsVerticalPadding = 8.dp,
        items = listOf(
            getTimelineGroup(prefState = playerPreferencesState, prefs = playerPreferences),
            getSubtitlesGroup(prefState = playerPreferencesState, prefs = playerPreferences),
            getDisplayGroup(prefState = playerPreferencesState, prefs = playerPreferences)
        )
    )
}

@OptIn(UnstableApi::class)
@Composable
private fun getTimelineGroup(
    prefState: DataStoreState<PlayerPreferences>,
    prefs: PlayerPreferences
): Preference.PreferenceCategory {
    return Preference.PreferenceCategory(
        title = stringResource(Res.string.preferences_player_timeline),
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

                title = stringResource(Res.string.preferences_player_seenthreshold),
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
                title = stringResource(Res.string.preferences_player_double_tap_length),
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
                title = stringResource(Res.string.player_pref_auto_play),
                value = prefs.autoPlay,
                subtitle = stringResource(Res.string.player_pref_auto_play_subtitle),
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
        title = stringResource(Res.string.label_subtitles),
        preferenceItems = listOf(
            Preference.PreferenceItem.TogglePreference(
                title = stringResource(Res.string.enable_subtitles_label),
                value = prefs.subtitlesEnabled,
                subtitle = stringResource(Res.string.enable_subtitles_description),
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
                title = stringResource(Res.string.subtitles_priority_label),
                subtitle = stringResource(Res.string.subtitles_priority_description),
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
        title = stringResource(Res.string.title_display),
        preferenceItems = listOf(
            Preference.PreferenceItem.TogglePreference(
                title = stringResource(Res.string.display_cutout_pref_title),
                value = prefs.ignoreCutout,
                subtitle = stringResource(Res.string.display_cutout_pref_subtitle),
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