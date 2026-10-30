package com.sf.tadami.ui.animeinfos.episode.cast

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.sf.tadami.R
import com.sf.tadami.ui.components.modal.ModalAction
import com.sf.tadami.ui.components.modal.TadaDialog
import com.sf.tadami.ui.utils.padding

@Composable
fun CastConnectionErrorDialog(
    onDismissRequest: () -> Unit,
) {
    TadaDialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(R.string.cast_error_dialog_title),
        dismiss = null,
        confirm = ModalAction(stringResource(android.R.string.ok), onClick = onDismissRequest),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small)) {
            Text(text = stringResource(R.string.cast_error_dialog_message))
            Text(text = stringResource(R.string.cast_error_dialog_cause_network))
            Text(text = stringResource(R.string.cast_error_dialog_cause_reboot))
        }
    }
}
