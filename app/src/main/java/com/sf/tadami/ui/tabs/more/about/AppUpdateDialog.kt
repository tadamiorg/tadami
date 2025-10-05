package com.sf.tadami.ui.tabs.more.about

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
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
import com.sf.tadami.ui.main.AppUpdaterUiState
import com.sf.tadami.ui.utils.padding

@Composable
fun AppUpdateDialog(
    onDismissRequest : () -> Unit,
    uiState : AppUpdaterUiState
) {
    val context = LocalContext.current
    CustomAlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            DefaultDialogConfirmButton(text = R.string.download) {
                AppUpdateWorker.startNow(context, uiState.updateInfos!!.getDownloadLink())
                onDismissRequest()
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
                            linkStyle = TextLinkStyles(style=SpanStyle(color = MaterialTheme.colorScheme.primary)),
                        ),
                    ),
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