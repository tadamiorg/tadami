package com.sf.tadami.ui.category

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.ArrowDropUp
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.sf.tadami.R
import com.sf.tadami.domain.category.Category
import com.sf.tadami.ui.components.grid.EmptyScreen
import com.sf.tadami.ui.components.material.ExtendedFloatingActionButton
import com.sf.tadami.ui.components.modal.TadaDestructiveDialog
import com.sf.tadami.ui.components.modal.TadaInputDialog
import com.sf.tadami.ui.components.topappbar.TadaTopAppBar
import com.sf.tadami.ui.components.widgets.ScrollbarLazyColumn
import com.sf.tadami.ui.utils.padding

/** The user's categories, in their order: create, rename, delete and move them */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    navController: NavHostController,
    viewModel: CategoryViewModel = viewModel(),
) {
    val categories by viewModel.categories.collectAsState()
    val dialog by viewModel.dialog.collectAsState()

    Scaffold(
        topBar = {
            TadaTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.label_categories),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text(text = stringResource(R.string.action_add)) },
                icon = { Icon(imageVector = Icons.Outlined.Add, contentDescription = null) },
                onClick = { viewModel.showDialog(CategoryDialog.Create()) },
            )
        },
    ) { contentPadding ->
        val list = categories ?: return@Scaffold
        if (list.isEmpty()) {
            EmptyScreen(
                modifier = Modifier.padding(contentPadding),
                message = stringResource(R.string.categories_empty),
            )
        } else {
            ScrollbarLazyColumn(
                contentPadding = PaddingValues(
                    start = MaterialTheme.padding.medium,
                    end = MaterialTheme.padding.medium,
                    top = contentPadding.calculateTopPadding() + MaterialTheme.padding.small,
                    // Room for the button over the last item
                    bottom = contentPadding.calculateBottomPadding() + 88.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
            ) {
                itemsIndexed(list, key = { _, category -> category.id }) { index, category ->
                    CategoryListItem(
                        modifier = Modifier.animateItem(),
                        category = category,
                        canMoveUp = index > 0,
                        canMoveDown = index < list.lastIndex,
                        onMoveUp = { viewModel.moveCategory(category, up = true) },
                        onMoveDown = { viewModel.moveCategory(category, up = false) },
                        onRename = { viewModel.showDialog(CategoryDialog.Rename(category)) },
                        onDelete = { viewModel.showDialog(CategoryDialog.Delete(category)) },
                    )
                }
            }
        }
    }

    when (val current = dialog) {
        is CategoryDialog.Create -> CategoryNameDialog(
            title = stringResource(R.string.action_add_category),
            confirmText = R.string.action_add,
            initialName = "",
            error = current.error,
            onNameChanged = viewModel::clearDialogError,
            onDismissRequest = viewModel::dismissDialog,
            onConfirm = viewModel::createCategory,
        )
        is CategoryDialog.Rename -> CategoryNameDialog(
            title = stringResource(R.string.action_rename_category),
            confirmText = null,
            initialName = current.category.name,
            error = current.error,
            onNameChanged = viewModel::clearDialogError,
            onDismissRequest = viewModel::dismissDialog,
            onConfirm = { viewModel.renameCategory(current.category, it) },
        )
        is CategoryDialog.Delete -> TadaDestructiveDialog(
            onDismissRequest = viewModel::dismissDialog,
            title = stringResource(R.string.action_delete_category),
            message = current.error?.let { categoryErrorText(it) }
                ?: stringResource(R.string.delete_category_confirmation, current.category.name),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = { viewModel.deleteCategory(current.category) },
        )
        null -> {}
    }
}

@Composable
private fun CategoryListItem(
    category: Category,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(
                start = MaterialTheme.padding.medium,
                top = MaterialTheme.padding.medium,
                end = MaterialTheme.padding.medium,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = Icons.AutoMirrored.Outlined.Label, contentDescription = null)
            Text(
                text = category.name,
                modifier = Modifier.padding(start = MaterialTheme.padding.medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                Icon(imageVector = Icons.Outlined.ArrowDropUp, contentDescription = stringResource(R.string.action_move_up))
            }
            IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                Icon(imageVector = Icons.Outlined.ArrowDropDown, contentDescription = stringResource(R.string.action_move_down))
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onRename) {
                Icon(imageVector = Icons.Outlined.Edit, contentDescription = stringResource(R.string.action_rename_category))
            }
            IconButton(onClick = onDelete) {
                Icon(imageVector = Icons.Outlined.Delete, contentDescription = stringResource(R.string.action_delete_category))
            }
        }
    }
}

/** The name of a new or renamed category. The interactors check it: [error] says why they refused it */
@Composable
private fun CategoryNameDialog(
    title: String,
    confirmText: Int?,
    initialName: String,
    error: CategoryError?,
    onNameChanged: () -> Unit,
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    TadaInputDialog(
        title = title,
        initialValue = initialName,
        confirmLabel = stringResource(confirmText ?: android.R.string.ok),
        onConfirm = onConfirm,
        onDismissRequest = onDismissRequest,
        icon = Icons.AutoMirrored.Outlined.Label,
        label = stringResource(R.string.category_name),
        error = error?.let { categoryErrorText(it) },
        // The interactors check it
        canConfirm = { true },
        onValueChange = { onNameChanged() },
    )
}

@Composable
private fun categoryErrorText(error: CategoryError): String {
    return stringResource(
        when (error) {
            CategoryError.EMPTY_NAME -> R.string.category_name_empty
            CategoryError.NAME_EXISTS -> R.string.category_name_exists
            CategoryError.INTERNAL -> R.string.category_error
        }
    )
}
