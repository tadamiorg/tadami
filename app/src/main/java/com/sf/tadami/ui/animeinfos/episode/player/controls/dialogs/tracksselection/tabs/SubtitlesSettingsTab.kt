package com.sf.tadami.ui.animeinfos.episode.player.controls.dialogs.tracksselection.tabs

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.media3.common.util.UnstableApi
import com.sf.tadami.R
import com.sf.tadami.preferences.model.DataStoreState
import com.sf.tadami.preferences.model.Preference
import com.sf.tadami.preferences.model.rememberDataStoreState
import com.sf.tadami.preferences.player.PlayerPreferences
import com.sf.tadami.ui.components.screens.ScreenTabContent
import com.sf.tadami.ui.tabs.more.settings.components.PreferenceParser
import com.sf.tadami.ui.tabs.more.settings.screens.appearance.OutlinedNumericChooser
import com.sf.tadami.ui.tabs.more.settings.widget.PrefsHorizontalPadding
import com.sf.tadami.ui.utils.padding

@OptIn(UnstableApi::class)
@Composable()
fun subtitleSettingsTab() : ScreenTabContent {
    val playerPreferencesState = rememberDataStoreState(PlayerPreferences)
    val playerPreferences by playerPreferencesState.value.collectAsState()
    return ScreenTabContent(
        titleRes = R.string.label_settings,
    ) { contentPadding: PaddingValues, _ ->
        PreferenceParser(
            customPrefsVerticalPadding = MaterialTheme.padding.medium,
            modifier = Modifier.padding(contentPadding),
            items = listOf(
                getSubtitlesGroup(prefState = playerPreferencesState, prefs = playerPreferences)
            )
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
fun getSubtitlesGroup(
    prefState: DataStoreState<PlayerPreferences>,
    prefs: PlayerPreferences
): Preference.PreferenceCategory {
    return Preference.PreferenceCategory(
        title = stringResource(id = R.string.label_subtitles),
        preferenceItems = listOf(
            Preference.PreferenceItem.CustomPreference(
                title = stringResource(id = R.string.pref_subtitles_appearance)
            ) {
                var textSize by remember {
                    mutableStateOf(prefs.subtitleTextSize)
                }

                Column {

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = PrefsHorizontalPadding)
                    ) {

                        OutlinedNumericChooser(
                            label = stringResource(R.string.size),
                            placeholder = "20",
                            suffix = "",
                            value = textSize,
                            step = 1,
                            min = 1,
                            onValueChanged = {
                                textSize = it
                                prefState.setValue(
                                    prefs.copy(
                                        subtitleTextSize = it
                                    )
                                )
                            },
                        )
                    }
                }
            },
        )
    )
}