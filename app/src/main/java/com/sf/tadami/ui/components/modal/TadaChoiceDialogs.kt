package com.sf.tadami.ui.components.modal

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sf.tadami.R

data class ChoiceOption<T>(
    val value: T,
    val label: String,
    val enabled: Boolean = true,
)

/** Tapping an option applies it and closes the dialog: no buttons. */
@Composable
fun <T> TadaSingleChoiceDialog(
    title: String,
    options: List<ChoiceOption<T>>,
    selected: T?,
    onSelect: (T) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TadaDialog(
        onDismissRequest = onDismissRequest,
        title = title,
        modifier = modifier,
        padBody = false,
        dismiss = null,
    ) {
        options.forEach { option ->
            ModalChoiceRow(
                label = option.label,
                control = ChoiceControl.Radio(option.value == selected),
                enabled = option.enabled,
                onClick = {
                    if (ModalRules.singleChoiceApplies(selected, option.value)) onSelect(option.value)
                    onDismissRequest()
                },
            )
            Spacer(Modifier.height(4.dp))
        }
    }
}

/**
 * Checkboxes, applied by the confirm button.
 *
 * @param confirmEnabled whether the selection can be applied
 */
@Composable
fun <T> TadaMultiChoiceDialog(
    title: String,
    options: List<ChoiceOption<T>>,
    selected: Set<T>,
    onConfirm: (Set<T>) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    confirmLabel: String = stringResource(android.R.string.ok),
    confirmEnabled: (Set<T>) -> Boolean = { true },
) {
    var selection by remember { mutableStateOf(selected) }
    TadaDialog(
        onDismissRequest = onDismissRequest,
        title = title,
        modifier = modifier,
        padBody = false,
        dismiss = ModalAction(stringResource(R.string.action_cancel), onClick = onDismissRequest),
        confirm = ModalAction(confirmLabel, enabled = confirmEnabled(selection)) { onConfirm(selection) },
    ) {
        options.forEach { option ->
            ModalChoiceRow(
                label = option.label,
                control = ChoiceControl.Check(option.value in selection),
                enabled = option.enabled,
                onClick = { selection = if (option.value in selection) selection - option.value else selection + option.value },
            )
        }
    }
}
