package com.sf.tadami.ui.tabs.more.settings.screens.data

import coil3.compose.LocalPlatformContext
import com.sf.tadami.utils.formatFileSize
import com.sf.tadami.network.player.VideoCache
import com.sf.tadami.ui.PlatformUi
import com.sf.tadami.BackgroundJobs
import com.sf.tadami.storage.StorageFiles
import org.jetbrains.compose.resources.StringResource
import com.sf.tadami.utils.Log
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MultiChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.navigation.NavHostController
import coil3.SingletonImageLoader
import com.sf.tadami.storage.StorageFile
import com.sf.tadami.shared.resources.Res
import com.sf.tadami.shared.resources.*
import com.sf.tadami.data.providers.FoldersProvider
import com.sf.tadami.navigation.graphs.settings.DataSettingsRoutes
import com.sf.tadami.preferences.backup.BackupPreferences
import com.sf.tadami.preferences.model.DataStoreState
import com.sf.tadami.preferences.model.Preference
import com.sf.tadami.preferences.model.rememberDataStoreState
import com.sf.tadami.preferences.storage.StoragePreferences
import com.sf.tadami.ui.tabs.more.settings.components.PreferenceScreen
import com.sf.tadami.ui.tabs.more.settings.widget.BasePreference
import com.sf.tadami.ui.tabs.more.settings.widget.PrefsHorizontalPadding
import com.sf.tadami.ui.utils.UiToasts
import com.sf.tadami.ui.utils.displayablePath
import com.sf.tadami.ui.utils.relativeTimeSpanString
import com.sf.tadami.utils.DeviceUtil
import kotlinx.collections.immutable.persistentListOf
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class DataPreferencesScreen(
    private val navController: NavHostController
) : PreferenceScreen {
    override val title: StringResource = Res.string.settings_tab_library_data_preferences_title

    override val backHandler: (() -> Unit) = { navController.navigateUp() }

    @Composable
    override fun getPreferences(): List<Preference> {
        val backupPreferencesState = rememberDataStoreState(BackupPreferences)
        val backupPreferences by backupPreferencesState.value.collectAsState()

        val storagePreferencesState = rememberDataStoreState(StoragePreferences)
        val storagePreferences by storagePreferencesState.value.collectAsState()

        return listOf(
            getStorageLocationPref(prefState = storagePreferencesState, prefs = storagePreferences),
            Preference.PreferenceItem.InfoPreference(stringResource(Res.string.pref_storage_location_info)),
            getBackupAndRestoreGroup(prefState = backupPreferencesState, prefs = backupPreferences),
            getDataGroup()
        )
    }

    @Composable
    private fun getStorageLocationPref(
        prefs: StoragePreferences,
        prefState: DataStoreState<StoragePreferences>
    ): Preference.PreferenceItem.TextPreference {
        val pickStorageLocation = storageLocationPicker(prefs, prefState)

        return Preference.PreferenceItem.TextPreference(
            title = stringResource(Res.string.pref_storage_location),
            subtitle = storageLocationText(prefs),
            onClick = pickStorageLocation,
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun getBackupAndRestoreGroup(
        prefs: BackupPreferences,
        prefState: DataStoreState<BackupPreferences>
    ): Preference.PreferenceCategory {
        val chooseBackup = Injekt.get<PlatformUi>().rememberFilePicker(
            stringResource(Res.string.file_select_backup)
        ) {
            if (it == null) {
                UiToasts.showToast(Res.string.file_null_uri_error)
                return@rememberFilePicker
            }

            val encodedUrl = URLEncoder.encode(
                it,
                StandardCharsets.UTF_8.toString()
            )

            navController.navigate("${DataSettingsRoutes.RESTORE_BACKUP}/${encodedUrl}")
        }

        return Preference.PreferenceCategory(
            title = stringResource(Res.string.preferences_backup_title),
            preferenceItems = persistentListOf(
                // Manual actions
                Preference.PreferenceItem.CustomPreference(
                    title = stringResource(Res.string.preferences_backup_title),
                ) {
                    BasePreference(
                        subcomponent = {
                            MultiChoiceSegmentedButtonRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(intrinsicSize = IntrinsicSize.Min)
                                    .padding(horizontal = PrefsHorizontalPadding),
                            ) {
                                SegmentedButton(
                                    modifier = Modifier.fillMaxHeight(),
                                    checked = false,
                                    onCheckedChange = {
                                        navController.navigate(DataSettingsRoutes.CREATE_BACKUP)
                                    },
                                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                                ) {
                                    Text(stringResource(Res.string.preferences_backup_create_title))
                                }
                                SegmentedButton(
                                    modifier = Modifier.fillMaxHeight(),
                                    checked = false,
                                    onCheckedChange = {
                                        if (!Injekt.get<BackgroundJobs>().isRestoreRunning()) {
                                            if (DeviceUtil.isMiui && DeviceUtil.isMiuiOptimizationDisabled()) {
                                                UiToasts.showToast(Res.string.restore_miui_warning)
                                            }

                                            chooseBackup()
                                        } else {
                                            UiToasts.showToast(Res.string.restore_in_progress)
                                        }
                                    },
                                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                                ) {
                                    Text(stringResource(Res.string.preferences_backup_restore_title))
                                }
                            }
                        },
                    )
                },

                // Automatic backups
                Preference.PreferenceItem.SelectPreference(
                    value = prefs.autoBackupInterval,
                    items = mapOf(
                        BackupPreferences.AutoBackupIntervalItems.DISABLED to stringResource(Res.string.preferences_library_auto_updates_disabled),
                        BackupPreferences.AutoBackupIntervalItems.DAILY to stringResource(Res.string.preferences_library_auto_updates_daily),
                        BackupPreferences.AutoBackupIntervalItems.DAILY_2 to stringResource(Res.string.preferences_library_auto_updates_daily2),
                        BackupPreferences.AutoBackupIntervalItems.DAILY_3 to stringResource(Res.string.preferences_library_auto_updates_daily3),
                        BackupPreferences.AutoBackupIntervalItems.WEEKLY to stringResource(Res.string.preferences_library_auto_updates_weekly),
                    ),
                    title = stringResource(Res.string.preferences_backup_frequency),
                    onValueChanged = {
                        prefState.setValue(
                            prefs.copy(
                                autoBackupInterval = it
                            )
                        )
                        Injekt.get<BackgroundJobs>().scheduleAutoBackups(it)
                        true
                    }
                ),
                Preference.PreferenceItem.InfoPreference(
                    stringResource(Res.string.backup_info) + "\n\n" +
                            stringResource(
                                Res.string.last_auto_backup_info,
                                relativeTimeSpanString(prefs.autoBackupLastTimestamp)
                            ),
                ),
            ),
        )
    }

    @Composable
    private fun getDataGroup(
        videoCache: VideoCache = Injekt.get()
    ): Preference.PreferenceCategory {
        val context = LocalPlatformContext.current
        val imageLoader = remember { SingletonImageLoader.get(context) }
        var readableImagesCacheSize by remember {
            mutableStateOf(
                formatFileSize(imageLoader.diskCache?.size ?: 0
                )
            )
        }
        var readableVideosCacheSize by remember {
            mutableStateOf(
                formatFileSize(videoCache.cacheSpace
                )
            )
        }

        return Preference.PreferenceCategory(
            title = stringResource(Res.string.pref_storage_usage),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.CustomPreference(
                    title = stringResource(Res.string.pref_storage_usage),
                ) {
                    BasePreference(
                        subcomponent = {
                            StorageInfo(
                                modifier = Modifier.padding(horizontal = PrefsHorizontalPadding),
                            )
                        },
                    )
                },

                Preference.PreferenceItem.TextPreference(
                    title = stringResource(Res.string.pref_clear_images_cache),
                    subtitle = stringResource(Res.string.used_cache, readableImagesCacheSize),
                    onClick = {
                        try {
                            imageLoader.diskCache?.clear()
                            UiToasts.showToast(Res.string.cache_deleted)

                        } catch (e: Throwable) {
                            Log.d("CacheDeletion", e.stackTraceToString())
                            UiToasts.showToast(Res.string.cache_delete_error)
                        } finally {
                            readableImagesCacheSize =
                                formatFileSize(imageLoader.diskCache?.size ?: 0)
                        }
                    },
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(Res.string.pref_clear_videos_cache),
                    subtitle = stringResource(Res.string.used_cache, readableVideosCacheSize),
                    onClick = {
                        try {
                            videoCache.clearCache()
                            UiToasts.showToast(Res.string.cache_deleted)

                        } catch (e: Throwable) {
                            Log.d("CacheDeletion", e.stackTraceToString())
                            UiToasts.showToast(Res.string.cache_delete_error)
                        } finally {
                            readableVideosCacheSize =
                                formatFileSize(videoCache.cacheSpace
                                )
                        }
                    },
                ),
            ),
        )
    }


    companion object {
        @Composable
        fun storageLocationPicker(
            storagePrefs: StoragePreferences,
            storagePrefsState: DataStoreState<StoragePreferences>
        ): () -> Unit {
            return Injekt.get<PlatformUi>().rememberFolderPicker { uri ->
                Injekt.get<StorageFiles>().fromUri(uri)?.let {
                    storagePrefsState.setValue(storagePrefs.copy(storageDir = it.uri.toString()))
                }
            }
        }

        @Composable
        fun storageLocationText(
            storageDirPref: StoragePreferences,
            foldersProviders: FoldersProvider = Injekt.get()
        ): String {
            if (storageDirPref.storageDir == foldersProviders.path()) {
                return stringResource(Res.string.no_location_set)
            }

            return remember(storageDirPref) {
                val file = Injekt.get<StorageFiles>().fromUri(storageDirPref.storageDir)
                file?.displayablePath
            } ?: stringResource(Res.string.invalid_location, storageDirPref.storageDir)
        }
    }
}