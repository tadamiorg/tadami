package com.sf.tadami.ui.tabs.more.settings.screens.appearance

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.view.MotionEvent
import androidx.annotation.OptIn
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.RemoveCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavHostController
import com.sf.tadami.R
import com.sf.tadami.preferences.appearance.AppearancePreferences
import com.sf.tadami.preferences.appearance.ThemeMode
import com.sf.tadami.preferences.appearance.setAppCompatDelegateThemeMode
import com.sf.tadami.preferences.model.DataStoreState
import com.sf.tadami.preferences.model.Preference
import com.sf.tadami.preferences.model.rememberDataStoreState
import com.sf.tadami.preferences.player.PlayerPreferences
import com.sf.tadami.ui.tabs.more.settings.components.PreferenceScreen
import com.sf.tadami.ui.tabs.more.settings.widget.AppThemeModePreference
import com.sf.tadami.ui.tabs.more.settings.widget.AppThemePreference
import com.sf.tadami.ui.tabs.more.settings.widget.PrefsHorizontalPadding
import kotlinx.coroutines.delay

class AppearancePreferencesScreen(
    navController: NavHostController,
) : PreferenceScreen {

    override val title: Int = R.string.label_appearance

    override val backHandler: (() -> Unit) = {
        navController.navigateUp()
    }

    @OptIn(UnstableApi::class)
    @Composable
    override fun getPreferences(): List<Preference> {
        val appearancePreferencesState = rememberDataStoreState(AppearancePreferences)
        val appearancePreferences by appearancePreferencesState.value.collectAsState()

        val playerPreferencesState = rememberDataStoreState(PlayerPreferences)
        val playerPreferences by playerPreferencesState.value.collectAsState()

        return listOf(
            getThemeGroup(prefState = appearancePreferencesState, prefs = appearancePreferences),
            getSubtitlesGroup(prefState = playerPreferencesState, prefs = playerPreferences)

        )
    }

    @Composable
    fun getThemeGroup(
        prefState: DataStoreState<AppearancePreferences>,
        prefs: AppearancePreferences
    ): Preference.PreferenceCategory {
        val context = LocalContext.current
        return Preference.PreferenceCategory(
            title = stringResource(id = R.string.label_theme),
            preferenceItems = listOf(
                Preference.PreferenceItem.CustomPreference(
                    title = stringResource(id = R.string.label_theme)
                ){
                    Column {
                        AppThemeModePreference(
                            value = prefs.themeMode,
                            onItemClick = {
                                prefState.setValue(prefs.copy(themeMode = it))
                                setAppCompatDelegateThemeMode(it)
                            },
                        )

                        AppThemePreference(
                            value = prefs.appTheme,
                            amoled = prefs.themeDarkAmoled,
                            onItemClick = { prefState.setValue(prefs.copy(appTheme = it)) },
                        )
                    }
                },
                Preference.PreferenceItem.TogglePreference(
                    title = stringResource(id = R.string.dark_theme_pure_black),
                    value = prefs.themeDarkAmoled,
                    enabled = isDarkMode(context,prefs),
                    onValueChanged = { themeDarkAmoled ->
                        (context as? Activity)?.let { ActivityCompat.recreate(it) }
                        prefState.setValue(prefs.copy(themeDarkAmoled = themeDarkAmoled))
                        true
                    }
                ),
            )
        )
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
}

private fun isDarkMode(context : Context,prefs : AppearancePreferences) : Boolean{
    val currentNightMode: Int = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    return when{
        prefs.themeMode == ThemeMode.DARK -> true
        prefs.themeMode == ThemeMode.SYSTEM && currentNightMode == Configuration.UI_MODE_NIGHT_YES -> true
        else -> false
    }
}

@Composable
fun OutlinedNumericChooser(
    label: String,
    placeholder: String,
    suffix: String,
    value: Int,
    step: Int,
    min: Int? = null,
    onValueChanged: (Int) -> Unit,
) {
    var currentValue = value

    val updateValue: (Boolean) -> Unit = {
        currentValue += if (it) step else -step

        if (min != null){
            currentValue = if (currentValue < min) min else currentValue
        }

        onValueChanged(currentValue)
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        RepeatingIconButton(
            onClick = { updateValue(false) },
        ) { Icon(imageVector = Icons.Outlined.RemoveCircle, contentDescription = null) }

        OutlinedTextField(
            value = "%d".format(currentValue),
            modifier = Modifier.widthIn(min = 140.dp),

            onValueChange = {
                val calculatedValue = it.trim().replace(Regex("[^-\\d.]"), "").toIntOrNull() ?: currentValue
                currentValue = calculatedValue
                onValueChanged(calculatedValue)
            },

            label = { Text(text = label) },
            placeholder = { Text(text = placeholder) },
            suffix = { Text(text = suffix) },

            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )

        RepeatingIconButton(
            onClick = { updateValue(true) },
        ) { Icon(imageVector = Icons.Outlined.AddCircle, contentDescription = null) }
    }
}

@kotlin.OptIn(ExperimentalComposeUiApi::class)
@Composable
fun RepeatingIconButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    maxDelayMillis: Long = 750,
    minDelayMillis: Long = 5,
    delayDecayFactor: Float = .25f,
    content: @Composable () -> Unit,
) {
    val currentClickListener by rememberUpdatedState(onClick)
    var pressed by remember { mutableStateOf(false) }

    IconButton(
        modifier = modifier.pointerInteropFilter {
            pressed = when (it.action) {
                MotionEvent.ACTION_DOWN -> true

                else -> false
            }

            true
        },
        onClick = {},
        enabled = enabled,
        interactionSource = interactionSource,
        content = content,
    )

    LaunchedEffect(pressed, enabled) {
        var currentDelayMillis = maxDelayMillis

        while (enabled && pressed) {
            currentClickListener()
            delay(currentDelayMillis)
            currentDelayMillis =
                (currentDelayMillis - (currentDelayMillis * delayDecayFactor))
                    .toLong().coerceAtLeast(minDelayMillis)
        }
    }
}