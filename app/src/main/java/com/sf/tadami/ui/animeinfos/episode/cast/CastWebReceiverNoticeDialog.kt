package com.sf.tadami.ui.animeinfos.episode.cast

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.sf.tadami.R
import com.sf.tadami.ui.components.dialog.alert.CustomAlertDialog
import com.sf.tadami.ui.components.dialog.alert.DefaultDialogConfirmButton
import com.sf.tadami.ui.components.widgets.ScrollbarLazyColumn
import com.sf.tadami.ui.utils.padding

/**
 * Shown once per session when the connected cast receiver announces itself as the *web* receiver:
 * playback may be less reliable than the native Tadami Terebi app, which the user can install on
 * Google TV / Android TV devices. "Don't show again" is persisted via CastPreferences.
 */
@Composable
fun CastWebReceiverNoticeDialog(
    onDismissRequest: (dontShowAgain: Boolean) -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val guideUrl = stringResource(R.string.cast_howto_guide_url)
    var dontShowAgain by remember { mutableStateOf(false) }
    CustomAlertDialog(
        onDismissRequest = { onDismissRequest(dontShowAgain) },
        title = {
            Text(text = stringResource(R.string.cast_web_receiver_notice_title))
        },
        text = {
            ScrollbarLazyColumn(
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
            ) {
                item { Text(text = stringResource(R.string.cast_web_receiver_notice_message)) }
                item { Text(text = stringResource(R.string.cast_web_receiver_notice_recommend)) }

                item { Spacer(Modifier.height(MaterialTheme.padding.small)) }
                item {
                    Text(
                        text = stringResource(R.string.cast_howto_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
                item { Text(text = stringResource(R.string.cast_howto_step_downloader)) }
                item { Text(text = stringResource(R.string.cast_howto_step_code)) }
                item {
                    Text(
                        text = stringResource(R.string.cast_howto_downloader_code),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                item { Text(text = stringResource(R.string.cast_howto_step_install)) }

                item { Spacer(Modifier.height(MaterialTheme.padding.small)) }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { dontShowAgain = !dontShowAgain },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = dontShowAgain,
                            onCheckedChange = { dontShowAgain = it },
                        )
                        Text(text = stringResource(R.string.cast_web_receiver_notice_dont_show_again))
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { uriHandler.openUri(guideUrl) }) {
                    Text(text = stringResource(R.string.cast_howto_open_guide))
                }
                DefaultDialogConfirmButton {
                    onDismissRequest(dontShowAgain)
                }
            }
        },
    )
}
