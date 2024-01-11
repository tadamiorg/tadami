package com.sf.tadami.ui.components.dialog.alert

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.sf.tadami.R
import com.sf.tadami.ui.utils.padding

@Composable
fun DefaultDialogCancelButton(
    @StringRes text : Int? = null,
    onDismissRequest: (() -> Unit)? = null
){
    TextButton(
        contentPadding = PaddingValues(bottom = MaterialTheme.padding.tiny, top = MaterialTheme.padding.tiny),
        onClick = onDismissRequest ?: LocalDismissRequest.current
    ) {
        Text(text = stringResource(id = text ?: R.string.player_screen_qd_cancel_btn))
    }
}

@Composable
fun DefaultDialogConfirmButton(
    @StringRes text : Int? = null,
    enabled : Boolean = true,
    onClick : () -> Unit,
) {
    TextButton(
        contentPadding = PaddingValues(bottom = MaterialTheme.padding.tiny, top = MaterialTheme.padding.tiny),
        enabled = enabled,
        onClick = onClick,
    ) {
        Text(text = stringResource(text ?: android.R.string.ok))
    }
}