package com.sf.tadami.ui.category

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.jetbrains.compose.resources.stringResource
import com.sf.tadami.shared.resources.Res
import com.sf.tadami.shared.resources.*
import com.sf.tadami.ui.components.modal.ChoiceControl
import com.sf.tadami.ui.components.modal.ModalAction
import com.sf.tadami.ui.components.modal.ModalBodyText
import com.sf.tadami.ui.components.modal.ModalChoiceRow
import com.sf.tadami.ui.components.modal.TadaDialog

/**
 * Checkboxes to put the anime in categories, from [initialSelection]. [onConfirm] gets the categories to add
 * them to and the ones to remove them from. [onEditCategories] opens the category screen, the dialog closes.
 */
@Composable
fun SetCategoriesDialog(
    initialSelection: List<CategoryCheckbox>,
    onDismissRequest: () -> Unit,
    onConfirm: (add: List<Long>, remove: List<Long>) -> Unit,
    onEditCategories: () -> Unit,
) {
    val editCategories = {
        onDismissRequest()
        onEditCategories()
    }

    if (initialSelection.isEmpty()) {
        TadaDialog(
            onDismissRequest = onDismissRequest,
            title = stringResource(Res.string.action_set_categories),
            confirm = ModalAction(stringResource(Res.string.action_edit), onClick = editCategories),
        ) {
            ModalBodyText(stringResource(Res.string.set_categories_none))
        }
        return
    }

    var selection by remember(initialSelection) { mutableStateOf(initialSelection) }

    TadaDialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(Res.string.action_set_categories),
        padBody = false,
        neutral = ModalAction(stringResource(Res.string.action_edit), onClick = editCategories),
        confirm = ModalAction(stringResource(Res.string.ok)) {
            val (add, remove) = selection.toChanges()
            onConfirm(add, remove)
            onDismissRequest()
        },
    ) {
        selection.forEachIndexed { index, checkbox ->
            ModalChoiceRow(
                label = checkbox.category.visualName(),
                control = ChoiceControl.TriState(checkbox.state),
                onClick = {
                    selection = selection.toMutableList().apply { set(index, checkbox.next()) }
                },
            )
        }
    }
}
