package com.sf.tadami.ui.tabs.settings.screens.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.commandiron.wheel_picker_compose.WheelPicker
import com.sf.tadami.R
import com.sf.tadami.ui.components.dialog.alert.CustomAlertDialog
import com.sf.tadami.ui.components.dialog.alert.DefaultDialogCancelButton
import com.sf.tadami.ui.components.dialog.alert.DefaultDialogConfirmButton
import com.sf.tadami.ui.components.data.Action
import com.sf.tadami.ui.tabs.settings.components.PreferenceScreen
import com.sf.tadami.ui.tabs.settings.model.DataStoreState
import com.sf.tadami.ui.tabs.settings.model.Preference
import com.sf.tadami.ui.tabs.settings.model.rememberDataStoreState
import com.sf.tadami.ui.utils.defaultParser

object UPDATE_RESTRICTIONS_ITEMS{
    const val WIFI = "wifi"
    const val CELLULAR = "cellular"
    const val BATTERY = "battery"
}

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
        prefState : DataStoreState<LibraryPreferences>,
        prefs :  LibraryPreferences
    ) : Preference.PreferenceCategory {
        return Preference.PreferenceCategory(
            title = stringResource(id = R.string.preferences_library_category_updates),
            preferenceItems = listOf(
                Preference.PreferenceItem.SelectPreference(
                    value = prefs.autoUpdates,
                    title = stringResource(id = R.string.preferences_library_auto_updates),
                    items = mapOf(
                        0 to stringResource(id = R.string.preferences_library_auto_updates_disabled),
                        24 to stringResource(id = R.string.preferences_library_auto_updates_daily),
                        48 to stringResource(id = R.string.preferences_library_auto_updates_daily2),
                        72 to stringResource(id = R.string.preferences_library_auto_updates_daily3),
                        168 to stringResource(id = R.string.preferences_library_auto_updates_weekly),
                    ),
                    onValueChanged = {
                        prefState.setValue(
                            prefs.copy(
                                autoUpdates = it
                            )
                        )
                        true
                    }

                ),
                Preference.PreferenceItem.MultiSelectPreference(
                    value = prefs.updateRestrictions,
                    title = stringResource(id = R.string.preferences_library_update_restrictions),
                    subtitle = stringResource(id = R.string.preferences_library_update_restrictions_subtitle),
                    items = mapOf(
                        UPDATE_RESTRICTIONS_ITEMS.WIFI to stringResource(id = R.string.preferences_library_update_restrictions_wifionly),
                        UPDATE_RESTRICTIONS_ITEMS.CELLULAR to stringResource(id = R.string.preferences_library_update_restrictions_cellularonly),
                        UPDATE_RESTRICTIONS_ITEMS.BATTERY to stringResource(id = R.string.preferences_library_update_restrictions_battery),
                    ),
                    onValueChanged = {
                        prefState.setValue(
                            prefs.copy(
                                updateRestrictions = it
                            )
                        )
                        true
                    }

                )
            )
        )
    }

    @Composable
    fun getDisplayGroup(
        prefState : DataStoreState<LibraryPreferences>,
        prefs :  LibraryPreferences
    ) : Preference.PreferenceCategory
    {
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
        var portraitValue by rememberSaveable { mutableStateOf(initialPortrait) }
        var landscapeValue by rememberSaveable { mutableStateOf(initialLandscape) }

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
                        modifier = Modifier.size(maxWidth, maxHeight / 3),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    ) {}

                    val size = DpSize(width = maxWidth / 2, height = 128.dp)
                    Row {
                        WheelPicker(
                            count = 11,
                            size = size,
                            startIndex = portraitValue,
                            onScrollFinished = {
                                portraitValue = it
                                null
                            }
                        ) { index, snappedIndex ->
                            ColumnPickerLabel(index = index, snappedIndex = snappedIndex)
                        }
                        WheelPicker(
                            count = 11,
                            size = size,
                            startIndex = landscapeValue,
                            onScrollFinished = {
                                landscapeValue = it
                                null
                            }
                        ) { index, snappedIndex ->
                            ColumnPickerLabel(index = index, snappedIndex = snappedIndex)
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun ColumnPickerLabel(
    index: Int,
    snappedIndex: Int,
) {
    Text(
        modifier = Modifier.alpha(
            when (snappedIndex) {
                index + 1 -> 0.2f
                index -> 1f
                index - 1 -> 0.2f
                else -> 0.2f
            },
        ),
        text = index.defaultParser(),
        style = MaterialTheme.typography.titleMedium,
        maxLines = 1,
    )
}