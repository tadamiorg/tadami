package com.sf.tadami.ui.main

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.halilibo.richtext.commonmark.CommonmarkAstNodeParser
import com.halilibo.richtext.markdown.BasicMarkdown
import com.halilibo.richtext.ui.BlockQuoteGutter
import com.halilibo.richtext.ui.RichTextStyle
import com.halilibo.richtext.ui.material3.RichText
import com.halilibo.richtext.ui.string.RichTextStringStyle
import com.sf.tadami.R
import com.sf.tadami.notifications.appupdate.AppUpdateWorker
import com.sf.tadami.ui.components.dialog.alert.CustomAlertDialog
import com.sf.tadami.ui.components.dialog.alert.DefaultDialogCancelButton
import com.sf.tadami.ui.components.dialog.alert.DefaultDialogConfirmButton
import com.sf.tadami.ui.components.widgets.ScrollbarLazyColumn
import com.sf.tadami.ui.utils.padding

@Composable
fun AppUpdaterScreen(
    appUpdaterViewModel: AppUpdaterViewModel = viewModel()
) {

    val uiState by appUpdaterViewModel.appUpdaterUiState.collectAsState()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    if (uiState.shouldShowUpdateDialog && uiState.updateInfos?.info != null) {
        CustomAlertDialog(
            onDismissRequest = { appUpdaterViewModel.hideDialog() },
            confirmButton = {
                DefaultDialogConfirmButton(text = R.string.download) {
                    AppUpdateWorker.startNow(context, uiState.updateInfos!!.getDownloadLink())
                    appUpdaterViewModel.hideDialog()
                }
            },
            dismissButton = {
                DefaultDialogCancelButton(text = R.string.later)
            },
            title = {
                Text(text = "${stringResource(id = R.string.app_name)} ${uiState.updateInfos!!.version}")
            }
        ) {
            val blockQuotescolor = MaterialTheme.colorScheme.primary
            ScrollbarLazyColumn(
                scrollBarAlwaysOn = true
            ) {
                item {
                    RichText(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                bottom = MaterialTheme.padding.medium,
                                top = MaterialTheme.padding.extraSmall
                            ),
                        style = RichTextStyle(
                            blockQuoteGutter = BlockQuoteGutter.BarGutter(
                                color = { blockQuotescolor.copy(alpha = .55f) }
                            ),
                            stringStyle = RichTextStringStyle(
                                linkStyle = SpanStyle(color = MaterialTheme.colorScheme.primary),
                            ),
                        ),
                        linkClickHandler = {
                            uriHandler.openUri(it)
                        }
                    ) {
                        val parser = remember { CommonmarkAstNodeParser() }
                        val astNode = remember(parser) {
                            parser.parse(
                                uiState.updateInfos!!.info
                            )
                        }
                        BasicMarkdown(astNode)
                    }
                }
            }
        }
    }
}