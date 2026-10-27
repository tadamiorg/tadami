package com.sf.tadami.ui.animeinfos.episode.player.controls.dialogs.tracksselection.tabs

import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import com.sf.tadami.shared.resources.Res
import com.sf.tadami.shared.resources.*
import com.sf.tadami.preferences.model.DataStoreState
import com.sf.tadami.preferences.model.Preference
import com.sf.tadami.preferences.model.rememberDataStoreState
import com.sf.tadami.preferences.player.PlayerPreferences
import com.sf.tadami.ui.tabs.more.settings.components.PreferenceParser
import com.sf.tadami.ui.tabs.more.settings.screens.appearance.OutlinedNumericChooser
import com.sf.tadami.ui.tabs.more.settings.widget.PrefsHorizontalPadding
import com.sf.tadami.ui.utils.padding

/** The subtitle appearance, applied live. */
@Composable()
fun SubtitleSettingsTab() {
    val playerPreferencesState = rememberDataStoreState(PlayerPreferences)
    val playerPreferences by playerPreferencesState.value.collectAsState()
    PreferenceParser(
        customPrefsVerticalPadding = MaterialTheme.padding.medium,
        items = listOf(
            getSubtitlesGroup(prefState = playerPreferencesState, prefs = playerPreferences)
        )
    )
}

@Composable
fun getSubtitlesGroup(
    prefState: DataStoreState<PlayerPreferences>,
    prefs: PlayerPreferences
): Preference.PreferenceCategory {
    return Preference.PreferenceCategory(
        title = stringResource(Res.string.label_subtitles),
        preferenceItems = listOf(
            Preference.PreferenceItem.CustomPreference(
                title = stringResource(Res.string.pref_subtitles_appearance)
            ) {
                var textSize by remember { mutableStateOf(prefs.subtitleTextSize) }
                var fontWeight by remember { mutableStateOf(prefs.subtitleFontWeight) }
                var outlineWidth by remember { mutableStateOf(prefs.subtitleOutlineWidth) }
                var letterSpacing by remember { mutableStateOf(prefs.subtitleLetterSpacing) }

                Column {

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = PrefsHorizontalPadding)
                    ) {

                        OutlinedNumericChooser(
                            label = stringResource(Res.string.size),
                            placeholder = "20",
                            suffix = "",
                            value = textSize,
                            step = 1,
                            min = 1,
                            fieldWidth = 150.dp,
                            onValueChanged = {
                                textSize = it
                                prefState.setValue(prefs.copy(subtitleTextSize = it))
                            },
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = PrefsHorizontalPadding)
                    ) {

                        OutlinedNumericChooser(
                            label = stringResource(Res.string.pref_subtitle_weight),
                            placeholder = "700",
                            suffix = "",
                            value = fontWeight,
                            step = 100,
                            min = 100,
                            fieldWidth = 150.dp,
                            onValueChanged = {
                                fontWeight = it
                                prefState.setValue(prefs.copy(subtitleFontWeight = it))
                            },
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = PrefsHorizontalPadding)
                    ) {

                        OutlinedNumericChooser(
                            label = stringResource(Res.string.pref_subtitle_outline),
                            placeholder = "22",
                            suffix = "%",
                            value = outlineWidth,
                            step = 1,
                            min = 0,
                            fieldWidth = 150.dp,
                            onValueChanged = {
                                outlineWidth = it
                                prefState.setValue(prefs.copy(subtitleOutlineWidth = it))
                            },
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = PrefsHorizontalPadding)
                    ) {

                        OutlinedNumericChooser(
                            label = stringResource(Res.string.pref_subtitle_letter_spacing),
                            placeholder = "0",
                            suffix = "",
                            value = letterSpacing,
                            step = 1,
                            min = 0,
                            fieldWidth = 150.dp,
                            onValueChanged = {
                                letterSpacing = it
                                prefState.setValue(prefs.copy(subtitleLetterSpacing = it))
                            },
                        )
                    }
                }
            },
        )
    )
}