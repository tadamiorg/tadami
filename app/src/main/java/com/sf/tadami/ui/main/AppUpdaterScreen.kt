package com.sf.tadami.ui.main

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sf.tadami.R
import com.sf.tadami.notifications.appupdate.AppUpdateWorker
import com.sf.tadami.ui.components.dialog.alert.CustomAlertDialog
import com.sf.tadami.ui.components.dialog.alert.DefaultDialogCancelButton
import com.sf.tadami.ui.components.dialog.alert.DefaultDialogConfirmButton
import dev.jeziellago.compose.markdowntext.MarkdownText

@Composable
fun AppUpdaterScreen(
    appUpdaterViewModel: AppUpdaterViewModel = viewModel()
) {

    val uiState by appUpdaterViewModel.appUpdaterUiState.collectAsState()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scrollState = rememberScrollState()

    if (uiState.shouldShowUpdateDialog) {
        CustomAlertDialog(
            onDismissRequest = { appUpdaterViewModel.hideDialog() },
            confirmButton = {
                DefaultDialogConfirmButton(text = R.string.download) {
                    AppUpdateWorker.startNow(context,uiState.updateInfos!!.getDownloadLink())
                    appUpdaterViewModel.hideDialog()
                }
            },
            dismissButton = {
                DefaultDialogCancelButton(text = R.string.later) {
                    appUpdaterViewModel.hideDialog()
                }
            },
            title = {
                Text(text = "${stringResource(id = R.string.app_name)} ${uiState.updateInfos!!.version}")
            }
        ) {
            MarkdownText(
                modifier = Modifier.verticalScroll(scrollState),
                markdown = uiState.updateInfos!!.info,
                color = MaterialTheme.colorScheme.onSurface,
                onLinkClicked = {
                    uriHandler.openUri(it)
                }
            )
        }
    }
}