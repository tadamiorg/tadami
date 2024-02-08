package com.sf.tadami.ui.tabs.settings.widget

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.sf.tadami.R
import com.sf.tadami.ui.components.dialog.alert.CustomAlertDialog
import com.sf.tadami.ui.components.dialog.alert.DefaultDialogCancelButton
import com.sf.tadami.ui.utils.UiToasts
import com.sf.tadami.ui.utils.secondaryItemAlpha
import kotlinx.coroutines.launch

@Composable
fun EditTextPreferenceWidget(
    title: String,
    subtitle: String?,
    icon: ImageVector?,
    value: String,
    defaultValue: String?,
    onConfirm: (String) -> Boolean,
) {
    var isDialogShown by rememberSaveable { mutableStateOf(false) }

    TextPreference(
        title = title,
        subtitle = subtitle?.format(value),
        icon = icon,
        onPreferenceClick = { isDialogShown = true },
    )

    if (isDialogShown) {
        val scope = rememberCoroutineScope()
        val onDismissRequest = { isDialogShown = false }
        var textFieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
            mutableStateOf(TextFieldValue(value))
        }
        CustomAlertDialog(
            onDismissRequest = onDismissRequest,
            title = {
                Column {
                    Text(text = title)
                    if (defaultValue != null) {
                        val clipboardManager = LocalClipboardManager.current
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(
                                    id = R.string.default_string_formatted,
                                    defaultValue
                                ),
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.secondaryItemAlpha()
                            )
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(defaultValue))
                                    UiToasts.showToast(R.string.clipboard_copied)
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .secondaryItemAlpha(),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ContentCopy,
                                    modifier = Modifier.fillMaxWidth(),
                                    contentDescription = null
                                )
                            }
                        }
                    }
                }

            },
            text = {
                OutlinedTextField(
                    value = textFieldValue,
                    onValueChange = { textFieldValue = it },
                    trailingIcon = {
                        if (textFieldValue.text.isBlank()) {
                            Icon(imageVector = Icons.Filled.Error, contentDescription = null)
                        } else {
                            IconButton(onClick = { textFieldValue = TextFieldValue("") }) {
                                Icon(imageVector = Icons.Filled.Cancel, contentDescription = null)
                            }
                        }
                    },
                    isError = textFieldValue.text.isBlank(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            properties = DialogProperties(
                usePlatformDefaultWidth = true,
            ),
            confirmButton = {
                TextButton(
                    enabled = textFieldValue.text != value && textFieldValue.text.isNotBlank(),
                    onClick = {
                        scope.launch {
                            if (onConfirm(textFieldValue.text)) {
                                onDismissRequest()
                            }
                        }
                    },
                ) {
                    Text(text = stringResource(R.string.player_screen_qd_ok_btn))
                }
            },
            dismissButton = {
                DefaultDialogCancelButton()
            },
        )
    }
}