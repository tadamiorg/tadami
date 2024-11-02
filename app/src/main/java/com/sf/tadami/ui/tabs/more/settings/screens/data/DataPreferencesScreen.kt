package com.sf.tadami.ui.tabs.more.settings.screens.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.format.Formatter
import android.util.Log
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.navigation.NavHostController
import coil.annotation.ExperimentalCoilApi
import coil.imageLoader
import com.hippo.unifile.UniFile
import com.sf.tadami.R
import com.sf.tadami.data.providers.AndroidFoldersProvider
import com.sf.tadami.network.player.PlayerNetworkHelper
import com.sf.tadami.notifications.backup.BackupCreateWorker
import com.sf.tadami.notifications.backup.BackupRestoreWorker
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

class DataPreferencesScreen(
    navController: NavHostController
) : PreferenceScreen {
    override val title: Int = R.string.preferences_backup_title

    override val backHandler: (() -> Unit) = { navController.navigateUp() }

    @Composable
    override fun getPreferences(): List<Preference> {
        val backupPreferencesState = rememberDataStoreState(BackupPreferences)
        val backupPreferences by backupPreferencesState.value.collectAsState()

        val storagePreferencesState = rememberDataStoreState(StoragePreferences)
        val storagePreferences by storagePreferencesState.value.collectAsState()

        return listOf(
            getStorageLocationPref(prefState = storagePreferencesState, prefs = storagePreferences),
            Preference.PreferenceItem.InfoPreference(stringResource(R.string.pref_storage_location_info)),
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
            title = stringResource(R.string.pref_storage_location),
            subtitle = storageLocationText(prefs),
            onClick = {
                try {
                    pickStorageLocation.launch(null)
                } catch (e: ActivityNotFoundException) {
                    UiToasts.showToast(R.string.file_picker_error)
                }
            },
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun getBackupAndRestoreGroup(
        prefs: BackupPreferences,
        prefState: DataStoreState<BackupPreferences>
    ): Preference.PreferenceCategory {
        val context = LocalContext.current

        val chooseBackup = rememberLauncherForActivityResult(
            object : ActivityResultContracts.GetContent() {
                override fun createIntent(context: Context, input: String): Intent {
                    val intent = super.createIntent(context, input)
                    return Intent.createChooser(
                        intent,
                        context.getString(R.string.file_select_backup)
                    )
                }
            },
        ) {
            if (it == null) {
                UiToasts.showToast(R.string.file_null_uri_error)
                return@rememberLauncherForActivityResult
            }

            // TODO navigate to restore backup screen
        }

        return Preference.PreferenceCategory(
            title = stringResource(R.string.preferences_backup_title),
            preferenceItems = persistentListOf(
                // Manual actions
                Preference.PreferenceItem.CustomPreference(
                    title = stringResource(R.string.preferences_backup_title),
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
                                        // TODO navigate to create backup screen
                                    },
                                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                                ) {
                                    Text(stringResource(R.string.preferences_backup_create_title))
                                }
                                SegmentedButton(
                                    modifier = Modifier.fillMaxHeight(),
                                    checked = false,
                                    onCheckedChange = {
                                        if (!BackupRestoreWorker.isRunning(context)) {
                                            if (DeviceUtil.isMiui && DeviceUtil.isMiuiOptimizationDisabled()) {
                                                UiToasts.showToast(R.string.restore_miui_warning)
                                            }

                                            // no need to catch because it's wrapped with a chooser
                                            chooseBackup.launch("*/*")
                                        } else {
                                            UiToasts.showToast(R.string.restore_in_progress)
                                        }
                                    },
                                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                                ) {
                                    Text(stringResource(R.string.preferences_backup_restore_title))
                                }
                            }
                        },
                    )
                },

                // Automatic backups
                Preference.PreferenceItem.SelectPreference(
                    value = prefs.autoBackupInterval,
                    items = mapOf(
                        BackupPreferences.AutoBackupIntervalItems.DISABLED to stringResource(id = R.string.preferences_library_auto_updates_disabled),
                        BackupPreferences.AutoBackupIntervalItems.DAILY to stringResource(id = R.string.preferences_library_auto_updates_daily),
                        BackupPreferences.AutoBackupIntervalItems.DAILY_2 to stringResource(id = R.string.preferences_library_auto_updates_daily2),
                        BackupPreferences.AutoBackupIntervalItems.DAILY_3 to stringResource(id = R.string.preferences_library_auto_updates_daily3),
                        BackupPreferences.AutoBackupIntervalItems.WEEKLY to stringResource(id = R.string.preferences_library_auto_updates_weekly),
                    ),
                    title = stringResource(id = R.string.preferences_backup_frequency),
                    onValueChanged = {
                        prefState.setValue(
                            prefs.copy(
                                autoBackupInterval = it
                            )
                        )
                        BackupCreateWorker.setupTask(context, it)
                        true
                    }
                ),
                Preference.PreferenceItem.InfoPreference(
                    stringResource(R.string.backup_info) + "\n\n" +
                            stringResource(
                                R.string.last_auto_backup_info,
                                relativeTimeSpanString(prefs.autoBackupLastTimestamp)
                            ),
                ),
            ),
        )
    }

    @OptIn(ExperimentalCoilApi::class)
    @Composable
    private fun getDataGroup(
        playerNetworkHelper: PlayerNetworkHelper = Injekt.get()
    ): Preference.PreferenceCategory {
        val context = LocalContext.current
        val imageLoader = remember { context.imageLoader }
        var readableImagesCacheSize by remember {
            mutableStateOf(
                Formatter.formatFileSize(
                    context,
                    imageLoader.diskCache?.size ?: 0
                )
            )
        }
        var readableVideosCacheSize by remember {
            mutableStateOf(
                Formatter.formatFileSize(
                    context,
                    playerNetworkHelper.cache.cacheSpace
                )
            )
        }

        return Preference.PreferenceCategory(
            title = stringResource(R.string.pref_storage_usage),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.CustomPreference(
                    title = stringResource(R.string.pref_storage_usage),
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
                    title = stringResource(R.string.pref_clear_images_cache),
                    subtitle = stringResource(R.string.used_cache, readableImagesCacheSize),
                    onClick = {
                        try {
                            imageLoader.diskCache?.clear()
                            UiToasts.showToast(R.string.cache_deleted)

                        } catch (e: Throwable) {
                            Log.e("CacheDeletion", e.stackTraceToString())
                            UiToasts.showToast(R.string.cache_delete_error)
                        } finally {
                            readableImagesCacheSize =
                                Formatter.formatFileSize(context, imageLoader.diskCache?.size ?: 0)
                        }
                    },
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(R.string.pref_clear_videos_cache),
                    subtitle = stringResource(R.string.used_cache, readableVideosCacheSize),
                    onClick = {
                        try {
                            playerNetworkHelper.clearCache()
                            UiToasts.showToast(R.string.cache_deleted)

                        } catch (e: Throwable) {
                            Log.e("CacheDeletion", e.stackTraceToString())
                            UiToasts.showToast(R.string.cache_delete_error)
                        } finally {
                            readableVideosCacheSize =
                                Formatter.formatFileSize(
                                    context,
                                    playerNetworkHelper.cache.cacheSpace
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
        ): ManagedActivityResultLauncher<Uri?, Uri?> {
            val context = LocalContext.current

            return rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocumentTree(),
            ) { uri ->
                if (uri != null) {
                    val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    try {
                        context.contentResolver.takePersistableUriPermission(uri, flags)
                    } catch (e: SecurityException) {
                        Log.e("StorageLocationPicker", e.stackTraceToString())
                        UiToasts.showToast(R.string.file_picker_uri_permission_unsupported)
                    }

                    UniFile.fromUri(context, uri)?.let {
                        storagePrefsState.setValue(storagePrefs.copy(storageDir = it.uri.toString()))
                    }
                }
            }
        }

        @Composable
        fun storageLocationText(
            storageDirPref: StoragePreferences,
            foldersProviders: AndroidFoldersProvider = Injekt.get()
        ): String {
            val context = LocalContext.current

            if (storageDirPref.storageDir == foldersProviders.path()) {
                return stringResource(R.string.no_location_set)
            }

            return remember(storageDirPref) {
                val file = UniFile.fromUri(context, storageDirPref.storageDir.toUri())
                file?.displayablePath
            } ?: stringResource(R.string.invalid_location, storageDirPref.storageDir)
        }
    }
}