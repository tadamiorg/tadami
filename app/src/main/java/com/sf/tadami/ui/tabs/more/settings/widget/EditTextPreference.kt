package com.sf.tadami.ui.tabs.more.settings.widget

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import com.sf.tadami.R
import com.sf.tadami.ui.components.modal.TadaInputDialog
import com.sf.tadami.ui.utils.UiToasts
import com.sf.tadami.ui.utils.secondaryItemAlpha
import kotlinx.coroutines.launch

@Composable
fun EditTextPreferenceWidget(
    customPrefsVerticalPadding : Dp? = null,
    title: String,
    subtitle: String?,
    icon: ImageVector?,
    value: String,
    defaultValue: String?,
    onConfirm: (String) -> Boolean,
) {
    var isDialogShown by rememberSaveable { mutableStateOf(false) }

    TextPreference(
        customPrefsVerticalPadding = customPrefsVerticalPadding,
        title = title,
        subtitle = subtitle?.format(value),
        icon = icon,
        onPreferenceClick = { isDialogShown = true },
    )

    if (isDialogShown) {
        val scope = rememberCoroutineScope()
        val onDismissRequest = { isDialogShown = false }
        val clipboardManager = LocalClipboardManager.current
        TadaInputDialog(
            title = title,
            subtitle = defaultValue?.let { stringResource(id = R.string.default_string_formatted, it) },
            headerAction = defaultValue?.let {
                {
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(it))
                            UiToasts.showToast(R.string.clipboard_copied)
                        },
                        modifier = Modifier.secondaryItemAlpha(),
                    ) {
                        Icon(imageVector = Icons.Outlined.ContentCopy, contentDescription = null)
                    }
                }
            },
            initialValue = value,
            confirmLabel = stringResource(R.string.player_screen_qd_ok_btn),
            canConfirm = { it != value && it.isNotBlank() },
            isError = { it.isBlank() },
            trailingIcon = { text, setText ->
                if (text.isBlank()) {
                    Icon(imageVector = Icons.Filled.Error, contentDescription = null)
                } else {
                    IconButton(onClick = { setText("") }) {
                        Icon(imageVector = Icons.Filled.Cancel, contentDescription = null)
                    }
                }
            },
            onConfirm = { text ->
                scope.launch {
                    if (onConfirm(text)) {
                        onDismissRequest()
                    }
                }
            },
            onDismissRequest = onDismissRequest,
        )
    }
}
