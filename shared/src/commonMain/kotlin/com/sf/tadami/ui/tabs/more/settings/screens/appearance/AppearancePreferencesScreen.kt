package com.sf.tadami.ui.tabs.more.settings.screens.appearance

import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.Injekt
import com.sf.tadami.ui.PlatformUi
import org.jetbrains.compose.resources.StringResource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.sf.tadami.shared.resources.Res
import com.sf.tadami.shared.resources.*
import com.sf.tadami.preferences.appearance.AppearancePreferences
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

    override val title: StringResource = Res.string.label_appearance

    override val backHandler: (() -> Unit) = {
        navController.navigateUp()
    }

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
        return Preference.PreferenceCategory(
            title = stringResource(Res.string.label_theme),
            preferenceItems = listOf(
                Preference.PreferenceItem.CustomPreference(
                    title = stringResource(Res.string.label_theme)
                ){
                    Column {
                        AppThemeModePreference(
                            value = prefs.themeMode,
                            onItemClick = {
                                prefState.setValue(prefs.copy(themeMode = it))
                                Injekt.get<PlatformUi>().applyThemeMode(it)
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
                    title = stringResource(Res.string.dark_theme_pure_black),
                    value = prefs.themeDarkAmoled,
                    enabled = isSystemInDarkTheme(),
                    onValueChanged = { themeDarkAmoled ->
                        prefState.setValue(prefs.copy(themeDarkAmoled = themeDarkAmoled))
                        true
                    }
                ),
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
                                label = stringResource(Res.string.size),
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

@Composable
fun OutlinedNumericChooser(
    label: String,
    placeholder: String,
    suffix: String,
    value: Int,
    step: Int,
    min: Int? = null,
    fieldWidth: Dp? = null,
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
            modifier = if (fieldWidth != null) Modifier.width(fieldWidth) else Modifier.widthIn(min = 140.dp),

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
        // Every event goes to the button, pressed until the next one: a move stops the repeat too
        modifier = modifier.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    pressed = event.type == PointerEventType.Press
                    event.changes.forEach { it.consume() }
                }
            }
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