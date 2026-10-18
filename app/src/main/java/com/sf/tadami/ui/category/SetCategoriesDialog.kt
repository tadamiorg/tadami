package com.sf.tadami.ui.category

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.triStateToggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.sf.tadami.R
import com.sf.tadami.ui.components.dialog.alert.CustomAlertDialog
import com.sf.tadami.ui.components.dialog.alert.DefaultDialogCancelButton
import com.sf.tadami.ui.components.dialog.alert.DefaultDialogConfirmButton
import com.sf.tadami.ui.utils.padding

/**
 * Checkboxes to put the anime in categories, from [initialSelection]. [onConfirm] gets the categories to add
 * them to and the ones to remove them from.
 */
@Composable
fun SetCategoriesDialog(
    initialSelection: List<CategoryCheckbox>,
    onDismissRequest: () -> Unit,
    onConfirm: (add: List<Long>, remove: List<Long>) -> Unit,
) {
    if (initialSelection.isEmpty()) {
        CustomAlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text(text = stringResource(R.string.action_set_categories)) },
            confirmButton = { DefaultDialogConfirmButton(onClick = onDismissRequest) },
        ) {
            Text(text = stringResource(R.string.set_categories_none))
        }
        return
    }

    var selection by remember(initialSelection) { mutableStateOf(initialSelection) }

    CustomAlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = stringResource(R.string.action_set_categories)) },
        dismissButton = { DefaultDialogCancelButton() },
        confirmButton = {
            DefaultDialogConfirmButton {
                val (add, remove) = selection.toChanges()
                onConfirm(add, remove)
                onDismissRequest()
            }
        },
    ) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            selection.forEachIndexed { index, checkbox ->
                CategoryCheckboxRow(
                    checkbox = checkbox,
                    onClick = {
                        selection = selection.toMutableList().apply { set(index, checkbox.next()) }
                    }
                )
            }
        }
    }
}

@Composable
private fun CategoryCheckboxRow(
    checkbox: CategoryCheckbox,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .triStateToggleable(
                state = checkbox.state,
                role = Role.Checkbox,
                onClick = onClick,
            )
            .minimumInteractiveComponentSize()
            .fillMaxWidth(),
    ) {
        TriStateCheckbox(
            state = checkbox.state,
            onClick = null,
        )
        Text(
            text = checkbox.category.visualName(),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = MaterialTheme.padding.large),
        )
    }
}
