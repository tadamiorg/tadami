package com.sf.tadami.ui.tabs.more.settings.screens.data.backup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.selection.SelectionContainer
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.sf.tadami.R
import com.sf.tadami.data.backup.RestoreOptions
import com.sf.tadami.preferences.model.Preference
import com.sf.tadami.ui.components.banners.WarningBanner
import com.sf.tadami.ui.components.grid.LazyColumnWithAction
import com.sf.tadami.ui.components.topappbar.TadaTopAppBar
import com.sf.tadami.ui.components.widgets.LabeledCheckbox
import com.sf.tadami.ui.components.widgets.SectionCard
import com.sf.tadami.ui.tabs.more.settings.components.PreferenceScreen
import com.sf.tadami.ui.utils.padding
import com.sf.tadami.utils.DeviceUtil

class RestoreBackupScreen(
    private val navController: NavHostController,
) : PreferenceScreen {

    override val title: Int = R.string.preferences_backup_restore_title

    override val backHandler: (() -> Unit) = { navController.navigateUp() }

    @Composable
    override fun getPreferences(): List<Preference> {
        return emptyList()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun getContent(
        restoreBackupViewModel: RestoreBackupViewModel = viewModel()
    ) {
        val context = LocalContext.current
        val uiState by restoreBackupViewModel.uiState.collectAsState()
        Scaffold(
            topBar = {
                TadaTopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.preferences_backup_restore_title),
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
                actionLabel = stringResource(R.string.preferences_backup_restore_title),
                actionEnabled = uiState.canRestore && uiState.options.canRestore(),
                onClickAction = {
                    restoreBackupViewModel.startRestore()
                    navController.popBackStack()
                },
            ) {
                if (DeviceUtil.isMiui && DeviceUtil.isMiuiOptimizationDisabled()) {
                    item {
                        WarningBanner(R.string.restore_miui_warning)
                    }
                }

                if (uiState.canRestore) {
                    item {
                        SectionCard {
                            RestoreOptions.options.forEach { option ->
                                LabeledCheckbox(
                                    label = stringResource(option.label),
                                    checked = option.getter(uiState.options),
                                    onCheckedChange = {
                                        restoreBackupViewModel.toggle(option.setter, it)
                                    },
                                )
                            }
                        }
                    }
                }

                if (uiState.error != null) {
                    errorMessageItem(uiState.error)
                }
            }
        }
    }

    private fun LazyListScope.errorMessageItem(
        error: Any?,
    ) {
        item {
            SectionCard {
                Column(
                    modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
                ) {
                    val msg = buildAnnotatedString {
                        when (error) {
                            is MissingRestoreComponents -> {
                                appendLine(stringResource(R.string.backup_restore_content_full))
                                if (error.sources.isNotEmpty()) {
                                    appendLine()
                                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                        appendLine(stringResource(R.string.backup_restore_missing_sources))
                                    }
                                    error.sources.joinTo(
                                        this,
                                        separator = "\n- ",
                                        prefix = "- ",
                                    )
                                }
                            }

                            is InvalidRestore -> {
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                    appendLine(stringResource(R.string.invalid_backup_file))
                                }
                                appendLine(error.uri.toString())

                                appendLine()

                                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                    appendLine(stringResource(R.string.invalid_backup_file_error))
                                }
                                appendLine(error.message)
                            }

                            else -> {
                                appendLine(error.toString())
                            }
                        }
                    }

                    SelectionContainer {
                        Text(text = msg)
                    }
                }
            }
        }
    }

    @Composable
    override fun Content() {
        getContent()
    }
}
