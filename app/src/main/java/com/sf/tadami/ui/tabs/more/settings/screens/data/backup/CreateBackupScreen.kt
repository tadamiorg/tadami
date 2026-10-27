package com.sf.tadami.ui.tabs.more.settings.screens.data.backup

import org.jetbrains.compose.resources.StringResource
import android.content.ActivityNotFoundException
import android.content.Intent
import com.sf.tadami.utils.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.sf.tadami.shared.resources.Res
import com.sf.tadami.shared.resources.*
import com.sf.tadami.data.backup.BackupOptions
import com.sf.tadami.data.backup.models.BackupUtils
import com.sf.tadami.notifications.backup.BackupCreateWorker
import com.sf.tadami.preferences.model.Preference
import com.sf.tadami.ui.components.banners.WarningBanner
import com.sf.tadami.ui.components.grid.LazyColumnWithAction
import com.sf.tadami.ui.components.topappbar.TadaTopAppBar
import com.sf.tadami.ui.components.widgets.LabeledCheckbox
import com.sf.tadami.ui.components.widgets.SectionCard
import com.sf.tadami.ui.tabs.more.settings.components.PreferenceScreen
import com.sf.tadami.ui.utils.UiToasts
import com.sf.tadami.utils.DeviceUtil
import kotlinx.collections.immutable.ImmutableList


class CreateBackupScreen(
    private val navController: NavHostController
) : PreferenceScreen {

    override val title: StringResource = Res.string.settings_tab_library_data_preferences_title

    override val backHandler: (() -> Unit) = { navController.navigateUp() }

    @Composable
    override fun getPreferences(): List<Preference> {
        return emptyList()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun getContent(
        createBackupViewModel: CreateBackupViewModel = viewModel()
    ) {
        val context = LocalContext.current
        val uiState by createBackupViewModel.uiState.collectAsState()

        val chooseBackupDir = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/*"),
        ) {
            if (it != null) {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        it,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                    )
                } catch (e: SecurityException) {
                    // Some file pickers can't grant it: the access the picker gave still writes the backup now
                    Log.d("CreateBackupScreen", e.stackTraceToString())
                }
                createBackupViewModel.createBackup(context, it)
                navController.popBackStack()
            }
        }

        Scaffold(
            topBar = {
                TadaTopAppBar(
                    title = {
                        Text(
                            text = stringResource(Res.string.preferences_backup_create_title),
                            style = MaterialTheme.typography.headlineSmall,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = null,
                            )
                        }
                    }
                )
            },
        ) { contentPadding ->
            LazyColumnWithAction(
                contentPadding = contentPadding,
                actionLabel = stringResource(Res.string.preferences_backup_create_title),
                actionEnabled = uiState.options.canCreate(),
                onClickAction = {
                    if (!BackupCreateWorker.isManualJobRunning(context)) {
                        try {
                            chooseBackupDir.launch(BackupUtils.getFilename())
                        } catch (e: ActivityNotFoundException) {
                            UiToasts.showToast(Res.string.file_picker_error)
                        }
                    } else {
                        UiToasts.showToast(Res.string.backup_in_progress)
                    }
                },
            ) {
                if (DeviceUtil.isMiui && DeviceUtil.isMiuiOptimizationDisabled()) {
                    item {
                        WarningBanner(Res.string.restore_miui_warning)
                    }
                }

                item {
                    SectionCard(Res.string.library_tab_title) {
                        Options(BackupOptions.libraryOptions, uiState, createBackupViewModel)
                    }
                }

                item {
                    SectionCard(Res.string.label_settings) {
                        Options(BackupOptions.settingsOptions,uiState, createBackupViewModel)
                    }
                }
            }
        }
    }

    @Composable
    override fun Content() {
        getContent()
    }

    @Composable
    private fun Options(
        options: ImmutableList<BackupOptions.Entry>,
        state: CreateBackupUiState,
        model: CreateBackupViewModel,
    ) {
        options.forEach { option ->
            LabeledCheckbox(
                label = stringResource(option.label),
                checked = option.getter(state.options),
                onCheckedChange = {
                    model.toggle(option.setter, it)
                },
                enabled = option.enabled(state.options),
            )
        }
    }
}
