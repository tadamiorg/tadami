package com.sf.tadami.ui.tabs.browse.tabs.extensions.repos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
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
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
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
import com.sf.tadami.ui.components.dialog.alert.DialogCheckBoxRow
import com.sf.tadami.ui.components.material.ExtendedFloatingActionButton
import com.sf.tadami.ui.components.topappbar.TadaTopAppBar
import com.sf.tadami.ui.components.widgets.ScrollbarLazyColumn
import com.sf.tadami.ui.tabs.browse.tabs.extensions.components.groupedFingerprint
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
            if (repos.none(ExtensionRepos::isOfficial)) {
                item(key = "add-official") {
                    OutlinedButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem(),
                        onClick = viewModel::addOfficialRepo,
                    ) {
                        Icon(imageVector = Icons.Outlined.Verified, contentDescription = null)
                        Text(
                            modifier = Modifier.padding(start = MaterialTheme.padding.small),
                            text = stringResource(R.string.ext_repo_add_official),
                        )
                    }
                }
            }
            items(repos, key = { it }) { repoUrl ->
                RepoListItem(
                    modifier = Modifier.animateItem(),
                    name = repoDisplayName(repoUrl, repoMetas[repoUrl]),
                    repoUrl = repoUrl,
                    isOfficial = ExtensionRepos.isOfficial(repoUrl),
                    onRemove = { viewModel.showRemoveDialog(repoUrl) },
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
            onConfirm = { trustKey -> viewModel.addRepo(current.repo, trustKey) },
        )
        is RepoDialog.Remove -> RemoveRepoDialog(
            name = repoDisplayName(current.repoUrl, repoMetas[current.repoUrl]),
            isOfficial = ExtensionRepos.isOfficial(current.repoUrl),
            onDismissRequest = viewModel::dismissDialog,
            onConfirm = { viewModel.removeRepo(current.repoUrl) },
        )
        null -> {}
    }
}

/** A repo, the official one marked as such. */
@Composable
private fun RepoListItem(
    name: String,
    repoUrl: String,
    isOfficial: Boolean,
    onRemove: () -> Unit,
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        modifier = Modifier.weight(1f, fill = false),
                        text = name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (isOfficial) {
                        Icon(
                            modifier = Modifier
                                .padding(start = MaterialTheme.padding.extraSmall)
                                .size(18.dp),
                            imageVector = Icons.Outlined.Verified,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Text(
                    modifier = Modifier.secondaryItemAlpha(),
                    text = repoUrl,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onRemove) {
                Icon(imageVector = Icons.Outlined.Delete, contentDescription = stringResource(R.string.ext_repo_remove))
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
                // Not disabled while it's checked: it would lose the focus
                readOnly = state.checking,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
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

/**
 * Before adding [repo], read: what it is and what it lists, and the key its extensions are signed
 * with, if it gives it, to trust them.
 */
@Composable
private fun ConfirmAddRepoDialog(
    repo: FetchedRepo,
    onDismissRequest: () -> Unit,
    onConfirm: (trustKey: Boolean) -> Unit,
) {
    val fingerprint = repo.meta?.signingKeyFingerprint
    // On by default, as Mihon trusts the key of a repo it adds
    var trustKey by rememberSaveable { mutableStateOf(true) }
    CustomAlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.fillMaxWidth(),
        title = { Text(text = stringResource(R.string.ext_repo_confirm_title)) },
        dismissButton = { DefaultDialogCancelButton() },
        confirmButton = {
            DefaultDialogConfirmButton(text = R.string.action_add) {
                onConfirm(fingerprint != null && trustKey)
            }
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small)) {
            Column {
                Text(text = repoDisplayName(repo.url, repo.meta), style = MaterialTheme.typography.titleSmall)
                Text(text = repo.url, style = MaterialTheme.typography.bodySmall)
            }
            Text(text = pluralStringResource(R.plurals.ext_repo_extensions_count, repo.extensionsCount, repo.extensionsCount))
            if (fingerprint != null) {
                Column {
                    Text(text = stringResource(R.string.ext_trust_fingerprint), style = MaterialTheme.typography.labelMedium)
                    SelectionContainer {
                        Text(
                            text = groupedFingerprint(fingerprint),
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                DialogCheckBoxRow(
                    label = stringResource(R.string.ext_repo_trust_key),
                    isSelected = trustKey,
                    onSelected = { trustKey = !it },
                )
            }
            Text(text = stringResource(R.string.ext_repo_confirm_warning))
        }
    }
}

/** Before removing a repo, [isOfficial] for the official one, whose extensions are no longer updated then. */
@Composable
private fun RemoveRepoDialog(
    name: String,
    isOfficial: Boolean,
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
            if (isOfficial) {
                Text(text = stringResource(R.string.ext_repo_remove_official_warning), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
