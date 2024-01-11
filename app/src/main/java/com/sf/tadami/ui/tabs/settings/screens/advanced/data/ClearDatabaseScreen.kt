package com.sf.tadami.ui.tabs.settings.screens.advanced.data


import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.FlipToBack
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.sf.tadami.R
import com.sf.tadami.data.interactors.sources.GetSourcesWithNonLibraryAnime
import com.sf.tadami.data.sources.SourceWithCount
import com.sf.tadami.network.api.online.AnimeCatalogueSource
import com.sf.tadami.network.api.online.StubSource
import com.sf.tadami.ui.components.data.Action
import com.sf.tadami.ui.components.dialog.alert.CustomAlertDialog
import com.sf.tadami.ui.components.dialog.alert.DefaultDialogCancelButton
import com.sf.tadami.ui.components.dialog.alert.DefaultDialogConfirmButton
import com.sf.tadami.ui.components.grid.EmptyScreen
import com.sf.tadami.ui.components.topappbar.ActionItem
import com.sf.tadami.ui.components.widgets.FastScrollLazyColumn
import com.sf.tadami.ui.tabs.settings.components.PreferenceScreen
import com.sf.tadami.ui.tabs.settings.model.Preference
import com.sf.tadami.ui.utils.UiToasts
import com.sf.tadami.ui.utils.selectedBackground
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class ClearDatabaseScreen(val navController: NavHostController) : PreferenceScreen {

    override val title: Int = R.string.pref_clear_database
    override val backHandler: (() -> Unit) = { navController.navigateUp() }

    @Composable
    override fun getPreferences(): List<Preference> {
        return emptyList()
    }

    @Composable
    fun getTopBarActions(
        onSelectAll: () -> Unit,
        onInvertSelect: () -> Unit
    ) {
        val actions = remember {
            listOf(
                Action.Vector(
                    title = R.string.stub_text,
                    icon = Icons.Outlined.SelectAll,
                    onClick = onSelectAll,
                ),
                Action.Vector(
                    title = R.string.stub_text,
                    icon = Icons.Outlined.FlipToBack,
                    onClick = onInvertSelect,
                ),
            )
        }
        actions.forEach {
            ActionItem(action = it)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun getContent(
        clearDatabaseViewModel: ClearDatabaseViewModel = viewModel()
    ) {
        val coroutineScope = rememberCoroutineScope()
        val uiState by clearDatabaseViewModel.uiState.collectAsState()
        if (uiState.showConfirmation) {
            CustomAlertDialog(
                onDismissRequest = clearDatabaseViewModel::hideConfirmation,
                confirmButton = {
                    DefaultDialogConfirmButton(
                        text = android.R.string.ok
                    ) {
                        coroutineScope.launch {
                            clearDatabaseViewModel.removeAnimeBySourceIds()
                            clearDatabaseViewModel.clearSelection()
                            clearDatabaseViewModel.hideConfirmation()
                            UiToasts.showToast(R.string.clear_database_completed)
                        }
                    }
                },
                dismissButton = {
                    DefaultDialogCancelButton(text = R.string.action_cancel)
                },
                text = {
                    Text(text = stringResource(R.string.clear_database_confirmation))
                },
            )
        }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = getTitle(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = backHandler) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = stringResource(R.string.stub_text),
                            )
                        }
                    },
                    actions = {
                        getTopBarActions(
                            onInvertSelect = clearDatabaseViewModel::invertSelection,
                            onSelectAll = clearDatabaseViewModel::selectAll
                        )
                    },
                )
            },
            content = { contentPadding ->
                if (uiState.items.isEmpty()) {
                    EmptyScreen(
                        message = stringResource(R.string.database_clean),
                        modifier = Modifier.padding(contentPadding),
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .padding(contentPadding)
                            .fillMaxSize(),
                    ) {
                        FastScrollLazyColumn(
                            modifier = Modifier.weight(1f),
                        ) {
                            items(uiState.items) { sourceWithCount ->
                                ClearDatabaseItem(
                                    source = sourceWithCount.source,
                                    count = sourceWithCount.count,
                                    isSelected = uiState.selection.contains(sourceWithCount.id),
                                    onClickSelect = {
                                        clearDatabaseViewModel.toggleSelection(
                                            sourceWithCount.source
                                        )
                                    },
                                )
                            }
                        }

                        Divider(thickness = 1.dp)

                        Button(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .fillMaxWidth(),
                            onClick = clearDatabaseViewModel::showConfirmation,
                            enabled = uiState.selection.isNotEmpty(),
                        ) {
                            Text(
                                text = stringResource(R.string.action_delete),
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    }
                }
            },
        )
    }

    @Composable
    private fun ClearDatabaseItem(
        source: AnimeCatalogueSource,
        count: Long,
        isSelected: Boolean,
        onClickSelect: () -> Unit,
    ) {
        Row(
            modifier = Modifier
                .selectedBackground(isSelected)
                .clickable(onClick = onClickSelect)
                .padding(horizontal = 8.dp)
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (source is StubSource) {
                Image(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .height(40.dp)
                        .aspectRatio(1f),
                )
            } else {
                Image(
                    painter = painterResource(id = source.getIconRes()!!),
                    contentDescription = null,
                    modifier = Modifier
                        .height(40.dp)
                        .aspectRatio(1f),
                )
            }

            Column(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .weight(1f),
            ) {
                Text(
                    text = source.name,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(R.string.clear_database_source_item_count, count),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onClickSelect() },
            )
        }
    }

    @Composable
    override fun Content() {
        getContent()
    }

    data class UiState(
        val selection: List<String> = emptyList(),
        val items: List<SourceWithCount> = emptyList(),
        val showConfirmation: Boolean = false
    )

    class ClearDatabaseViewModel() : ViewModel() {
        private val getSourcesWithNonLibraryAnime: GetSourcesWithNonLibraryAnime = Injekt.get()

        private val _uiState = MutableStateFlow(UiState())
        val uiState = _uiState.asStateFlow()

        init {
            viewModelScope.launch(Dispatchers.IO) {
                getSourcesWithNonLibraryAnime.subscribe()
                    .collectLatest { list ->
                        _uiState.update { old ->
                            val items = list.sortedBy { it.name }
                            old.copy(items = items)
                        }
                    }
            }
        }

        suspend fun removeAnimeBySourceIds() {
            getSourcesWithNonLibraryAnime.delete(_uiState.value.selection)
        }


        fun toggleSelection(source: AnimeCatalogueSource) = _uiState.update { state ->
            val mutableList = state.selection.toMutableList()
            if (mutableList.contains(source.id)) {
                mutableList.remove(source.id)
            } else {
                mutableList.add(source.id)
            }
            state.copy(selection = mutableList)
        }

        fun clearSelection() = _uiState.update { state ->
            state.copy(selection = emptyList())
        }

        fun selectAll() = _uiState.update { state ->
            state.copy(selection = state.items.fastMap { it.id })
        }

        fun invertSelection() = _uiState.update { state ->
            state.copy(
                selection = state.items
                    .fastMap { it.id }
                    .filterNot { it in state.selection },
            )
        }

        fun showConfirmation() = _uiState.update { state ->
            state.copy(showConfirmation = true)
        }

        fun hideConfirmation() = _uiState.update { state ->
            state.copy(showConfirmation = false)
        }
    }
}
