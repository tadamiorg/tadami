package com.sf.tadami.ui.animeinfos.episode.cast

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.sf.tadami.R
import com.sf.tadami.ui.components.dialog.alert.CustomAlertDialog
import com.sf.tadami.ui.components.dialog.alert.DefaultDialogConfirmButton
import com.sf.tadami.ui.utils.padding

@Composable
fun CastConnectionErrorDialog(
    onDismissRequest: () -> Unit,
) {
    CustomAlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = stringResource(R.string.cast_error_dialog_title))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small)) {
                Text(text = stringResource(R.string.cast_error_dialog_message))
                Text(text = stringResource(R.string.cast_error_dialog_cause_app))
                Text(text = stringResource(R.string.cast_error_dialog_cause_network))
                Text(text = stringResource(R.string.cast_error_dialog_cause_reboot))
            }
        },
        confirmButton = {
            DefaultDialogConfirmButton {
                onDismissRequest()
            }
        },
    )
}
