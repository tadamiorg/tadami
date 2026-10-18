package com.sf.tadami.ui.tabs.browse.tabs.extensions.repos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.sf.tadami.R
import com.sf.tadami.extension.api.ExtensionRepos
import com.sf.tadami.extension.api.FetchedRepo
import com.sf.tadami.ui.components.dialog.alert.CustomAlertDialog
import com.sf.tadami.ui.components.dialog.alert.DefaultDialogCancelButton
import com.sf.tadami.ui.components.dialog.alert.DefaultDialogConfirmButton
import com.sf.tadami.ui.components.material.ExtendedFloatingActionButton
import com.sf.tadami.ui.components.topappbar.TadaTopAppBar
import com.sf.tadami.ui.components.widgets.ScrollbarLazyColumn
import com.sf.tadami.ui.tabs.browse.tabs.extensions.components.repoDisplayName
import com.sf.tadami.ui.utils.padding
import com.sf.tadami.ui.utils.secondaryItemAlpha

/** The repos the extensions are found in: add and remove them, but the official one. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionReposScreen(
    navController: NavHostController,
    viewModel: ExtensionReposViewModel = viewModel(),
) {
    val repos by viewModel.repos.collectAsState()
    val repoMetas by viewModel.repoMetas.collectAsState()
    val dialog by viewModel.dialog.collectAsState()

    Scaffold(
        topBar = {
            TadaTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.ext_repos_title),
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
                onClick = viewModel::showAddDialog,
            )
        },
    ) { contentPadding ->
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
            items(repos, key = { it }) { repoUrl ->
                RepoListItem(
                    modifier = Modifier.animateItem(),
                    name = repoDisplayName(repoUrl, repoMetas[repoUrl]),
                    repoUrl = repoUrl,
                    onRemove = { viewModel.showRemoveDialog(repoUrl) }.takeUnless { ExtensionRepos.isOfficial(repoUrl) },
                )
            }
        }
    }

    when (val current = dialog) {
        is RepoDialog.Add -> AddRepoDialog(
            state = current,
            onDismissRequest = viewModel::dismissDialog,
            onSubmit = viewModel::submit,
        )
        is RepoDialog.ConfirmAdd -> ConfirmAddRepoDialog(
            repo = current.repo,
            onDismissRequest = viewModel::dismissDialog,
            onConfirm = { viewModel.addRepo(current.repo) },
        )
        is RepoDialog.Remove -> RemoveRepoDialog(
            name = repoDisplayName(current.repoUrl, repoMetas[current.repoUrl]),
            onDismissRequest = viewModel::dismissDialog,
            onConfirm = { viewModel.removeRepo(current.repoUrl) },
        )
        null -> {}
    }
}

/** A repo, [onRemove] null for the official one, which can't be removed. */
@Composable
private fun RepoListItem(
    name: String,
    repoUrl: String,
    onRemove: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = MaterialTheme.padding.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = MaterialTheme.padding.medium),
            ) {
                Text(text = name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    modifier = Modifier.secondaryItemAlpha(),
                    text = repoUrl,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (onRemove != null) {
                IconButton(onClick = onRemove) {
                    Icon(imageVector = Icons.Outlined.Delete, contentDescription = stringResource(R.string.ext_repo_remove))
                }
            } else {
                IconButton(onClick = {}, enabled = false) {
                    Icon(imageVector = Icons.Outlined.Verified, contentDescription = stringResource(R.string.ext_repo_official))
                }
            }
        }
    }
}

/** The URL of a repo to add, checked by [onSubmit]: [state] says why it was refused. */
@Composable
private fun AddRepoDialog(
    state: RepoDialog.Add,
    onDismissRequest: () -> Unit,
    onSubmit: (String) -> Unit,
) {
    var url by rememberSaveable { mutableStateOf(state.input) }
    val focusRequester = remember { FocusRequester() }
    val error = state.error?.let { stringResource(it, state.errorDetail.orEmpty()) }

    CustomAlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.fillMaxWidth(),
        title = { Text(text = stringResource(R.string.ext_repo_add_title)) },
        dismissButton = { DefaultDialogCancelButton() },
        confirmButton = {
            DefaultDialogConfirmButton(text = R.string.action_add, enabled = !state.checking && url.isNotBlank()) {
                onSubmit(url)
            }
        },
    ) {
        Column {
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text(text = stringResource(R.string.ext_repo_url)) },
                placeholder = { Text(text = "https://github.com/owner/repo") },
                isError = error != null,
                supportingText = error?.let { { Text(text = it) } },
                enabled = !state.checking,
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            )
            if (state.checking) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }

    LaunchedEffect(focusRequester) {
        focusRequester.requestFocus()
    }
}

/** Before adding [repo], read: what it is and what it lists. */
@Composable
private fun ConfirmAddRepoDialog(
    repo: FetchedRepo,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    CustomAlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.fillMaxWidth(),
        title = { Text(text = stringResource(R.string.ext_repo_confirm_title)) },
        dismissButton = { DefaultDialogCancelButton() },
        confirmButton = {
            DefaultDialogConfirmButton(text = R.string.action_add) {
                onConfirm()
            }
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small)) {
            Column {
                Text(text = repoDisplayName(repo.url, repo.meta), style = MaterialTheme.typography.titleSmall)
                Text(text = repo.url, style = MaterialTheme.typography.bodySmall)
            }
            Text(text = pluralStringResource(R.plurals.ext_repo_extensions_count, repo.extensionsCount, repo.extensionsCount))
            Text(text = stringResource(R.string.ext_repo_confirm_warning))
        }
    }
}

@Composable
private fun RemoveRepoDialog(
    name: String,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    CustomAlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.fillMaxWidth(),
        title = { Text(text = stringResource(R.string.ext_repo_remove_title)) },
        dismissButton = { DefaultDialogCancelButton() },
        confirmButton = {
            DefaultDialogConfirmButton(text = R.string.ext_repo_remove) {
                onConfirm()
            }
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small)) {
            Text(text = name, style = MaterialTheme.typography.titleSmall)
            Text(text = stringResource(R.string.ext_repo_remove_message))
        }
    }
}
