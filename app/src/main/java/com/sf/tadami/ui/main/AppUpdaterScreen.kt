package com.sf.tadami.ui.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.halilibo.richtext.markdown.Markdown
import com.halilibo.richtext.ui.RichTextStyle
import com.halilibo.richtext.ui.material3.Material3RichText
import com.halilibo.richtext.ui.string.RichTextStringStyle
import com.sf.tadami.R
import com.sf.tadami.notifications.appupdate.AppUpdateWorker
import com.sf.tadami.ui.components.dialog.alert.CustomAlertDialog
import com.sf.tadami.ui.components.dialog.alert.DefaultDialogCancelButton
import com.sf.tadami.ui.components.dialog.alert.DefaultDialogConfirmButton
import kotlinx.coroutines.launch

@Composable
fun AppUpdaterScreen(
    appUpdaterViewModel: AppUpdaterViewModel = viewModel()
) {

    var shouldShowUpdateDialog by rememberSaveable { mutableStateOf(false) }
    val updateInfos by appUpdaterViewModel.updateInfos.collectAsState()
    val context = LocalContext.current


    LaunchedEffect(updateInfos) {
        launch {
           if(updateInfos!=null) shouldShowUpdateDialog = true
        }
    }

    val scrollState = rememberScrollState()

    if (shouldShowUpdateDialog) {
        CustomAlertDialog(
            onDismissRequest = { shouldShowUpdateDialog = false },
            confirmButton = {
                DefaultDialogConfirmButton(text = R.string.download) {
                    AppUpdateWorker.startNow(context,updateInfos!!.getDownloadLink())
                    shouldShowUpdateDialog = false
                }
            },
            dismissButton = {
                DefaultDialogCancelButton(text = R.string.later) {
                    shouldShowUpdateDialog = false
                }
            },
            title = {
                Text(text = "${stringResource(id = R.string.app_name)} ${updateInfos!!.version}")
            },
            modifier = Modifier.fillMaxSize(),
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Column(modifier = Modifier.verticalScroll(state = scrollState)) {
                Material3RichText(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    style = RichTextStyle(
                        stringStyle = RichTextStringStyle(
                            linkStyle = SpanStyle(color = MaterialTheme.colorScheme.primary),
                        ),
                    ),
                ) {
                    Markdown(content = updateInfos!!.info)
                }
            }
        }
    }
}