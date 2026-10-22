package com.sf.tadami.ui.components.modal

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/**
 * One text field, focused when it opens. A tap outside is ignored once the text was changed and while
 * [busy] (it's being checked, the field is read-only and the confirm button shows a progress).
 *
 * @param icon the header's badge, also the field's leading icon
 * @param subtitle under the title, e.g. the default value
 * @param headerAction at the end of the header, e.g. copy the default value
 * @param trailingIcon at the end of the field, given the text and a way to change it
 * @param onValueChange each change of the text
 */
@Composable
fun TadaInputDialog(
    title: String,
    initialValue: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    subtitle: String? = null,
    headerAction: (@Composable () -> Unit)? = null,
    label: String? = null,
    placeholder: String? = null,
    error: String? = null,
    isError: (String) -> Boolean = { error != null },
    busy: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    canConfirm: (String) -> Boolean = { it.isNotBlank() },
    onValueChange: (String) -> Unit = {},
    trailingIcon: (@Composable (text: String, setText: (String) -> Unit) -> Unit)? = null,
) {
    var value by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(initialValue, TextRange(initialValue.length)))
    }
    val text = value.text
    val focusRequester = remember { FocusRequester() }
    val setText: (String) -> Unit = {
        value = TextFieldValue(it, TextRange(it.length))
        onValueChange(it)
    }

    TadaDialog(
        onDismissRequest = onDismissRequest,
        title = title,
        modifier = modifier,
        icon = icon,
        subtitle = subtitle,
        headerAction = headerAction,
        dismissOnClickOutside = ModalRules.inputDismissesOnClickOutside(text, initialValue, busy),
        confirm = ModalAction(confirmLabel, enabled = canConfirm(text), busy = busy) { onConfirm(text) },
    ) {
        TextField(
            value = value,
            onValueChange = {
                if (it.text != value.text) onValueChange(it.text)
                value = it
            },
            label = label?.let { { Text(it) } },
            placeholder = placeholder?.let { { Text(it) } },
            leadingIcon = icon?.let { { Icon(it, contentDescription = null) } },
            trailingIcon = trailingIcon?.let { { it(text, setText) } },
            isError = isError(text),
            supportingText = error?.let { { Text(it) } },
            readOnly = busy,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge,
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                errorContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            ),
            keyboardOptions = keyboardOptions,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
        )
        LaunchedEffect(focusRequester) {
            // Once the window has the focus
            delay(100.milliseconds)
            focusRequester.requestFocus()
        }
    }
}
