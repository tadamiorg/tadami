package com.sf.tadami.ui.animeinfos.episode.cast

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import com.sf.tadami.R
import com.sf.tadami.ui.components.modal.ModalAction
import com.sf.tadami.ui.components.modal.ModalCheckboxRow
import com.sf.tadami.ui.components.modal.TadaDialog
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
    TadaDialog(
        onDismissRequest = { onDismissRequest(dontShowAgain) },
        title = stringResource(R.string.cast_web_receiver_notice_title),
        neutral = ModalAction(stringResource(R.string.cast_howto_open_guide)) { uriHandler.openUri(guideUrl) },
        dismiss = null,
        confirm = ModalAction(stringResource(android.R.string.ok)) { onDismissRequest(dontShowAgain) },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small)) {
            Text(text = stringResource(R.string.cast_web_receiver_notice_message))
            Text(text = stringResource(R.string.cast_web_receiver_notice_recommend))
            Spacer(Modifier.height(MaterialTheme.padding.small))
            CastHowTo()
            ModalCheckboxRow(
                label = stringResource(R.string.cast_web_receiver_notice_dont_show_again),
                checked = dontShowAgain,
                onCheckedChange = { dontShowAgain = it },
            )
        }
    }
}
