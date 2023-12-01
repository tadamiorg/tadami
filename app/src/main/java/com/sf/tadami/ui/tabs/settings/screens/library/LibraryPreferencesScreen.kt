package com.sf.tadami.ui.tabs.settings.screens.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.commandiron.wheel_picker_compose.core.WheelPickerDefaults
import com.commandiron.wheel_picker_compose.core.WheelTextPicker
import com.sf.tadami.R
import com.sf.tadami.notifications.libraryupdate.LibraryUpdateWorker
import com.sf.tadami.ui.components.data.Action
import com.sf.tadami.ui.components.dialog.alert.CustomAlertDialog
import com.sf.tadami.ui.components.dialog.alert.DefaultDialogCancelButton
import com.sf.tadami.ui.components.dialog.alert.DefaultDialogConfirmButton
import com.sf.tadami.ui.tabs.settings.components.PreferenceScreen
import com.sf.tadami.ui.tabs.settings.model.DataStoreState
import com.sf.tadami.ui.tabs.settings.model.Preference
import com.sf.tadami.ui.tabs.settings.model.rememberDataStoreState
import com.sf.tadami.ui.utils.defaultParser
import com.sf.tadami.ui.utils.toPrefMultiCheckbox

class LibraryPreferencesScreen(
    navController: NavHostController,
) : PreferenceScreen {

    override val title: Int = R.string.preferences_library_title

    override val backHandler: (() -> Unit) = {
        navController.navigateUp()
    }

    override val topBarActions: List<Action> = listOf()

    @Composable
    override fun getPreferences(): List<Preference> {
        val libraryPreferencesState = rememberDataStoreState(LibraryPreferences)
        val libraryPreferences by libraryPreferencesState.value.collectAsState()

        return listOf(
            getDisplayGroup(prefState = libraryPreferencesState, prefs = libraryPreferences),
            getUpdateGroup(prefState = libraryPreferencesState, prefs = libraryPreferences)
        )
    }

    @Composable
    fun getUpdateGroup(
        prefState: DataStoreState<LibraryPreferences>,
        prefs: LibraryPreferences
    ): Preference.PreferenceCategory {
        val context = LocalContext.current
        return Preference.PreferenceCategory(
            title = stringResource(id = R.string.preferences_library_category_updates),
            preferenceItems = listOf(
                Preference.PreferenceItem.SelectPreference(
                    value = prefs.autoUpdateInterval,
                    title = stringResource(id = R.string.preferences_library_auto_updates),
                    items = mapOf(
                        LibraryPreferences.AutoUpdateIntervalItems.DISABLED to stringResource(id = R.string.preferences_library_auto_updates_disabled),
                        LibraryPreferences.AutoUpdateIntervalItems.DAILY to stringResource(id = R.string.preferences_library_auto_updates_daily),
                        LibraryPreferences.AutoUpdateIntervalItems.DAILY_2 to stringResource(id = R.string.preferences_library_auto_updates_daily2),
                        LibraryPreferences.AutoUpdateIntervalItems.DAILY_3 to stringResource(id = R.string.preferences_library_auto_updates_daily3),
                        LibraryPreferences.AutoUpdateIntervalItems.WEEKLY to stringResource(id = R.string.preferences_library_auto_updates_weekly),
                    ),
                    onValueChanged = {
                        prefState.setValue(
                            prefs.copy(
                                autoUpdateInterval = it
                            )
                        )
                        LibraryUpdateWorker.setupTask(context, it)
                        true
                    }

                ),
                Preference.PreferenceItem.MultiSelectPreference(
                    enabled = prefs.autoUpdateInterval > 0,
                    value = prefs.autoUpdateRestrictions,
                    title = stringResource(id = R.string.preferences_library_update_restrictions),
                    subtitle = stringResource(id = R.string.preferences_library_update_restrictions_subtitle),
                    items = mapOf(
                        LibraryPreferences.AutoUpdateRestrictionItems.WIFI to stringResource(id = R.string.preferences_library_update_restrictions_wifionly),
                        LibraryPreferences.AutoUpdateRestrictionItems.CHARGE to stringResource(id = R.string.preferences_library_update_restrictions_charge),
                        LibraryPreferences.AutoUpdateRestrictionItems.BATTERY to stringResource(id = R.string.preferences_library_update_restrictions_battery),
                    ).toPrefMultiCheckbox(),
                    onValueChanged = {
                        prefState.setValue(
                            prefs.copy(
                                autoUpdateRestrictions = it
                            )
                        )
                        LibraryUpdateWorker.setupTask(context)
                        true
                    }

                )
            )
        )
    }

    @Composable
    fun getDisplayGroup(
        prefState: DataStoreState<LibraryPreferences>,
        prefs: LibraryPreferences
    ): Preference.PreferenceCategory {
        var showDialog by rememberSaveable { mutableStateOf(false) }

        if (showDialog) {
            LibraryColumnsDialog(
                initialPortrait = prefs.portraitColumns,
                initialLandscape = prefs.landscapeColumns,
                onDismissRequest = { showDialog = false },
                onValueChanged = { portrait, landscape ->
                    prefState.setValue(
                        prefs.copy(
                            portraitColumns = portrait,
                            landscapeColumns = landscape
                        )
                    )
                    showDialog = false
                },
            )
        }

        return Preference.PreferenceCategory(
            title = stringResource(id = R.string.preferences_library_category_display),
            preferenceItems = listOf(
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(id = R.string.preferences_library_columns),
                    subtitle = "${stringResource(id = R.string.portrait)}: ${prefs.portraitColumns.defaultParser()}, " +
                            "${stringResource(id = R.string.landscape)}: ${prefs.landscapeColumns.defaultParser()}",
                    onClick = { showDialog = true },
                ),
            )
        )
    }

    @Composable
    private fun LibraryColumnsDialog(
        initialPortrait: Int,
        initialLandscape: Int,
        onDismissRequest: () -> Unit,
        onValueChanged: (portrait: Int, landscape: Int) -> Unit,
    ) {
        var portraitValue by rememberSaveable { mutableIntStateOf(initialPortrait) }
        var landscapeValue by rememberSaveable { mutableIntStateOf(initialLandscape) }

        CustomAlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text(text = stringResource(id = R.string.preferences_library_columns)) },
            dismissButton = {
                DefaultDialogCancelButton(onDismissRequest = onDismissRequest)
            },
            confirmButton = {
                DefaultDialogConfirmButton(
                    enabled = portraitValue != initialPortrait || landscapeValue != initialLandscape,
                ) {
                    onValueChanged(portraitValue, landscapeValue)
                }
            },
        ) {
            Column {
                Row {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.portrait),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Text(
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.landscape),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                BoxWithConstraints(
                    modifier = Modifier,
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        modifier = Modifier.size(maxWidth, 128.dp / 3),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    ) {}

                    val size = DpSize(width = maxWidth / 2, height = 128.dp)
                    val labels = (0..10).toList().map { value -> value.defaultParser() }
                    Row {
                        WheelTextPicker(
                            texts = labels,
                            rowCount = 3,
                            size = size,
                            startIndex = portraitValue,
                            onScrollFinished = {
                                portraitValue = it
                                null
                            },
                            selectorProperties = WheelPickerDefaults.selectorProperties(false)
                        )

                        WheelTextPicker(
                            texts = labels,
                            rowCount = 3,
                            size = size,
                            startIndex = landscapeValue,
                            onScrollFinished = {
                                landscapeValue = it
                                null
                            },
                            selectorProperties = WheelPickerDefaults.selectorProperties(false)
                        )
                    }
                }
            }
        }
    }
}