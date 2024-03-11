package com.sf.tadami.ui.tabs.settings.screens.backup

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.hippo.unifile.UniFile
import com.sf.tadami.R
import com.sf.tadami.data.backup.BackupCreateFlags
import com.sf.tadami.data.backup.models.Backup
import com.sf.tadami.notifications.backup.BackupCreateWorker
import com.sf.tadami.notifications.backup.BackupFileValidator
import com.sf.tadami.notifications.backup.BackupRestoreWorker
import com.sf.tadami.preferences.backup.BackupPreferences
import com.sf.tadami.preferences.model.DataStoreState
import com.sf.tadami.preferences.model.Preference
import com.sf.tadami.preferences.model.rememberDataStoreState
import com.sf.tadami.ui.tabs.settings.components.PreferenceScreen
import com.sf.tadami.ui.utils.UiToasts
import com.sf.tadami.ui.utils.toPrefMultiCheckbox

class BackupPreferencesScreen(
    navController : NavHostController
) : PreferenceScreen {
    override val title: Int = R.string.preferences_backup_title

    override val backHandler: (() -> Unit) = { navController.navigateUp() }

    @OptIn(ExperimentalPermissionsApi::class)
    @Composable
    override fun getPreferences(): List<Preference> {
        val backupPreferencesState = rememberDataStoreState(BackupPreferences)
        val backupPreferences by backupPreferencesState.value.collectAsState()
        val permissionState = rememberPermissionState(permission = Manifest.permission.WRITE_EXTERNAL_STORAGE)
        LaunchedEffect(Unit) {
            permissionState.launchPermissionRequest()
        }
        return listOf(
            getManualGroup(),
            getAutoGroup(prefState = backupPreferencesState,prefs = backupPreferences),
        )
    }

    @Composable
    private fun getManualGroup() : Preference.PreferenceCategory{
        val context = LocalContext.current
        var createBackupFlags by rememberSaveable {
            mutableStateOf(setOf<Int>())
        }

        // Create Activity Launcher
        val chooseBackupDir = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/*"),
        ) {
            if (it != null) {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )

                BackupCreateUtils.createBackup(context, it,createBackupFlags.fold(initial = 0, operation = { a, b -> a or b }))
                createBackupFlags = emptySet()
            }
        }

        // Restore Activity Launcher
        val chooseBackupRestore = rememberLauncherForActivityResult(
            object : ActivityResultContracts.GetContent() {
                override fun createIntent(context: Context, input: String): Intent {
                    val intent = super.createIntent(context, input)
                    return Intent.createChooser(intent, context.getString(R.string.file_select_backup))
                }
            },
        ) {
            if (it == null) {
                UiToasts.showToast(R.string.file_null_uri_error)
                return@rememberLauncherForActivityResult
            }

            try {
                BackupFileValidator().validate(context, it)
            } catch (e: Exception) {
                UiToasts.showToast("$it : ${e.message}")
                return@rememberLauncherForActivityResult
            }

            BackupRestoreWorker.start(context, it)
        }

        // String values to create backup
        val createElements = stringArrayResource(id = R.array.preferences_backup_create_fields)

        return Preference.PreferenceCategory(
            title = stringResource(id = R.string.preferences_backup_category_manual),
            preferenceItems = listOf(
                Preference.PreferenceItem.MultiSelectPreferenceInt(
                    value = BackupCreateFlags.SET,
                    items = mapOf(
                        BackupCreateFlags.BACKUP_DEFAULT_ANIME to (createElements[0] to false),
                        BackupCreateFlags.BACKUP_EPISODE to createElements[1],
                        BackupCreateFlags.BACKUP_APP_PREFS to createElements[2],
                        BackupCreateFlags.BACKUP_HISTORY to createElements[3],

                    ).toPrefMultiCheckbox(),
                    title = stringResource(id = R.string.preferences_backup_create_title),
                    subtitle = stringResource(id = R.string.preferences_backup_create_subtitle),
                    overrideOkButton = true,
                    onValueChanged = {
                        createBackupFlags = it
                        if (!BackupCreateWorker.isManualJobRunning(context)) {
                            try {
                                chooseBackupDir.launch(Backup.getFilename())
                            } catch (e: ActivityNotFoundException) {
                                UiToasts.showToast(R.string.file_picker_error)
                            }
                        } else {
                            UiToasts.showToast(R.string.backup_in_progress)
                        }
                        true
                    }
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(id = R.string.preferences_backup_restore_title),
                    subtitle = stringResource(id = R.string.preferences_backup_restore_subtitle),
                    onClick = {
                        if (!BackupRestoreWorker.isRunning(context)) {
                            // no need to catch because it's wrapped with a chooser
                            chooseBackupRestore.launch("*/*")
                        } else {
                            UiToasts.showToast(R.string.restore_in_progress)
                        }
                    }
                )
            )
        )
    }

    @Composable
    private fun getAutoGroup(
        prefState: DataStoreState<BackupPreferences>,
        prefs: BackupPreferences
    ) : Preference.PreferenceCategory{
        val context = LocalContext.current
        val pickLocation = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocumentTree(),
        ) { uri ->
            if (uri != null) {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION

                context.contentResolver.takePersistableUriPermission(uri, flags)
                val file = UniFile.fromUri(context, uri)
                file?.let {
                    prefState.setValue(prefs.copy(
                        autoBackupFolder = it.uri.toString()
                    ))
                }
            }
        }

        return Preference.PreferenceCategory(
            title = stringResource(id = R.string.preferences_backup_category_auto),
            preferenceItems = listOf(
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
                        prefState.setValue(prefs.copy(
                            autoBackupInterval = it
                        ))
                        BackupCreateWorker.setupTask(context, it)
                        true
                    }
                ),
                Preference.PreferenceItem.SelectPreference(
                    enabled = prefs.autoBackupInterval > 0,
                    value = prefs.autoBackupMaxFiles,
                    items = listOf(
                        BackupPreferences.AutoBackupMaxFiles.TWO,
                        BackupPreferences.AutoBackupMaxFiles.THREE,
                        BackupPreferences.AutoBackupMaxFiles.FOUR,
                        BackupPreferences.AutoBackupMaxFiles.FIVE,
                    ).associateWith { it.toString() },
                    title = stringResource(id = R.string.preferences_backup_maxfiles),
                    onValueChanged = {
                        prefState.setValue(prefs.copy(
                            autoBackupMaxFiles = it
                        ))
                        true
                    }
                ),
                Preference.PreferenceItem.TextPreference(
                    enabled = prefs.autoBackupInterval > 0,
                    title = stringResource(id = R.string.preferences_backup_directory),
                    subtitle = prefs.autoBackupFolder,
                    onClick = {

                        pickLocation.launch(null)
                    }
                )
            )
        )
    }
}

private data class InvalidRestore(
    val uri: Uri? = null,
    val message: String,
)