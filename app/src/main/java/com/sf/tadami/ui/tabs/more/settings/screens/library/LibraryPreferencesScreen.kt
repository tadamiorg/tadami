package com.sf.tadami.ui.tabs.more.settings.screens.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.commandiron.wheel_picker_compose.core.WheelPickerDefaults
import com.commandiron.wheel_picker_compose.core.WheelTextPicker
import com.sf.tadami.R
import com.sf.tadami.data.interactors.category.GetCategories
import com.sf.tadami.domain.category.Category
import com.sf.tadami.navigation.graphs.settings.SettingsRoutes
import com.sf.tadami.notifications.libraryupdate.LibraryUpdateWorker
import com.sf.tadami.preferences.library.LibraryPreferences
import com.sf.tadami.preferences.model.DataStoreState
import com.sf.tadami.preferences.model.Preference
import com.sf.tadami.preferences.model.rememberDataStoreState
import com.sf.tadami.ui.components.dialog.alert.CustomAlertDialog
import com.sf.tadami.ui.components.dialog.alert.DefaultDialogCancelButton
import com.sf.tadami.ui.components.dialog.alert.DefaultDialogConfirmButton
import com.sf.tadami.ui.tabs.more.settings.components.PreferenceScreen
import com.sf.tadami.ui.utils.defaultParser
import com.sf.tadami.ui.utils.toPrefMultiCheckbox
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class LibraryPreferencesScreen(
    private val navController: NavHostController,
) : PreferenceScreen {

    override val title: Int = R.string.preferences_library_title

    override val backHandler: (() -> Unit) = {
        navController.navigateUp()
    }

    @Composable
    override fun getPreferences(): List<Preference> {
        val libraryPreferencesState = rememberDataStoreState(LibraryPreferences)
        val libraryPreferences by libraryPreferencesState.value.collectAsState()
        val categories by remember { Injekt.get<GetCategories>().subscribe() }.collectAsState(initial = emptyList())

        return listOf(
            getDisplayGroup(prefState = libraryPreferencesState, prefs = libraryPreferences),
            getCategoriesGroup(prefState = libraryPreferencesState, prefs = libraryPreferences, categories = categories),
            getUpdateGroup(prefState = libraryPreferencesState, prefs = libraryPreferences, categories = categories)
        )
    }

    @Composable
    fun getCategoriesGroup(
        prefState: DataStoreState<LibraryPreferences>,
        prefs: LibraryPreferences,
        categories: List<Category>
    ): Preference.PreferenceCategory {
        val defaultCategoryItems = buildMap {
            put(LibraryPreferences.DEFAULT_CATEGORY_ASK, stringResource(id = R.string.preferences_library_default_category_ask))
            put(Category.UNCATEGORIZED_ID.toInt(), stringResource(id = R.string.category_default))
            categories.forEach { put(it.id.toInt(), it.name) }
        }
        return Preference.PreferenceCategory(
            title = stringResource(id = R.string.preferences_library_category_categories),
            preferenceItems = listOf(
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(id = R.string.action_edit_categories),
                    subtitle = pluralStringResource(
                        id = R.plurals.preferences_library_categories_count,
                        count = categories.size,
                        categories.size
                    ),
                    onClick = { navController.navigate(SettingsRoutes.CATEGORIES) }
                ),
                Preference.PreferenceItem.SelectPreference(
                    // A category deleted meanwhile is asked for
                    value = prefs.defaultCategory.takeIf { it in defaultCategoryItems } ?: LibraryPreferences.DEFAULT_CATEGORY_ASK,
                    title = stringResource(id = R.string.preferences_library_default_category),
                    items = defaultCategoryItems,
                    onValueChanged = {
                        prefState.setValue(
                            prefs.copy(
                                defaultCategory = it
                            )
                        )
                        true
                    }
                ),
                Preference.PreferenceItem.TogglePreference(
                    value = prefs.categorizedSort,
                    title = stringResource(id = R.string.preferences_library_categorized_sort),
                    subtitle = stringResource(id = R.string.preferences_library_categorized_sort_subtitle),
                    onValueChanged = {
                        prefState.setValue(
                            prefs.copy(
                                categorizedSort = it
                            )
                        )
                        true
                    }
                )
            )
        )
    }

    @Composable
    fun getUpdateGroup(
        prefState: DataStoreState<LibraryPreferences>,
        prefs: LibraryPreferences,
        categories: List<Category>
    ): Preference.PreferenceCategory {
        val context = LocalContext.current
        // The Default category too, for the anime in no category
        val categoryItems = buildMap {
            put(Category.UNCATEGORIZED_ID.toString(), stringResource(id = R.string.category_default))
            categories.forEach { put(it.id.toString(), it.name) }
        }
        return Preference.PreferenceCategory(
            title = stringResource(id = R.string.preferences_library_category_updates),
            preferenceItems = listOf(
                Preference.PreferenceItem.SelectPreference(
                    value = prefs.autoUpdateInterval,
                    title = stringResource(id = R.string.preferences_library_auto_updates),
                    items = mapOf(
                        LibraryPreferences.AutoUpdateIntervalItems.DISABLED to stringResource(id = R.string.preferences_library_auto_updates_disabled),
                        LibraryPreferences.AutoUpdateIntervalItems.TWELVE to stringResource(id = R.string.preferences_library_auto_updates_half_day),
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

                ),
                Preference.PreferenceItem.TogglePreference(
                    value = prefs.autoUpdateMetadata,
                    title = stringResource(id = R.string.preferences_library_auto_update_metadata),
                    subtitle = stringResource(id = R.string.preferences_library_auto_update_metadata_subtitle),
                    onValueChanged = {
                        prefState.setValue(
                            prefs.copy(
                                autoUpdateMetadata = it
                            )
                        )
                        true
                    }
                ),
                Preference.PreferenceItem.TogglePreference(
                    value = prefs.updateTitles,
                    title = stringResource(id = R.string.preferences_library_update_titles),
                    subtitle = stringResource(id = R.string.preferences_library_update_titles_subtitle),
                    onValueChanged = {
                        prefState.setValue(
                            prefs.copy(
                                updateTitles = it
                            )
                        )
                        true
                    }
                ),
                Preference.PreferenceItem.MultiSelectPreference(
                    value = prefs.updateCategories.filterTo(HashSet()) { it in categoryItems },
                    title = stringResource(id = R.string.preferences_library_update_categories_include),
                    items = categoryItems.toPrefMultiCheckbox(),
                    subtitleProvider = {
                        prefs.updateCategories.mapNotNull { categoryItems[it] }.takeIf { it.isNotEmpty() }?.joinToString()
                            ?: stringResource(id = R.string.preferences_library_update_categories_all)
                    },
                    onValueChanged = {
                        prefState.setValue(
                            prefs.copy(
                                updateCategories = it
                            )
                        )
                        true
                    }
                ),
                Preference.PreferenceItem.MultiSelectPreference(
                    value = prefs.updateCategoriesExclude.filterTo(HashSet()) { it in categoryItems },
                    title = stringResource(id = R.string.preferences_library_update_categories_exclude),
                    items = categoryItems.toPrefMultiCheckbox(),
                    subtitleProvider = {
                        prefs.updateCategoriesExclude.mapNotNull { categoryItems[it] }.takeIf { it.isNotEmpty() }?.joinToString()
                            ?: stringResource(id = R.string.none)
                    },
                    onValueChanged = {
                        prefState.setValue(
                            prefs.copy(
                                updateCategoriesExclude = it
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
                DefaultDialogCancelButton()
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