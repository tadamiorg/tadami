package com.sf.tadami.ui.animeinfos.episode.cast

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.sf.tadami.shared.resources.Res
import com.sf.tadami.shared.resources.*
import com.sf.tadami.ui.components.modal.ModalAction
import com.sf.tadami.ui.components.modal.TadaDialog
import com.sf.tadami.ui.utils.padding

@Composable
fun CastConnectionErrorDialog(
    onDismissRequest: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val guideUrl = stringResource(Res.string.cast_howto_guide_url)
    TadaDialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(Res.string.cast_error_dialog_title),
        neutral = ModalAction(stringResource(Res.string.cast_howto_open_guide)) { uriHandler.openUri(guideUrl) },
        dismiss = null,
        confirm = ModalAction(stringResource(Res.string.ok), onClick = onDismissRequest),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small)) {
            Text(text = stringResource(Res.string.cast_error_dialog_message))
            Text(text = stringResource(Res.string.cast_error_dialog_cause_app))
            Text(text = stringResource(Res.string.cast_error_dialog_cause_network))
            Text(text = stringResource(Res.string.cast_error_dialog_cause_reboot))
            Spacer(Modifier.height(MaterialTheme.padding.small))
            CastHowTo()
        }
    }
}

/** How to install Tadami Terebi on the TV, shared by the cast dialogs. */
@Composable
public fun CastHowTo() {
    Text(
        text = stringResource(Res.string.cast_howto_title),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
    )
    Text(text = stringResource(Res.string.cast_howto_step_downloader))
    Text(text = stringResource(Res.string.cast_howto_step_code))
    Text(
        text = stringResource(Res.string.cast_howto_downloader_code),
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
    )
    Text(text = stringResource(Res.string.cast_howto_step_install))
}
