package com.sf.tadami.ui.tabs.browse.tabs.extensions.filters

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.sf.tadami.ui.components.widgets.ContentLoader
import com.sf.tadami.ui.tabs.settings.widget.TogglePreference
import com.sf.tadami.utils.Lang

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExtensionsFilterComponent(
    contentPadding: PaddingValues,
    uiState: ExtensionsFilterUiState,
    onClickToggle: (String) -> Unit,
) {
    ContentLoader(isLoading = uiState is ExtensionsFilterUiState.Loading) {
        val successState = uiState as ExtensionsFilterUiState.Success
        LazyColumn(
            contentPadding = contentPadding,
        ) {

            items(successState.languages) { language ->
                TogglePreference(
                    modifier = Modifier.animateItemPlacement(),
                    title = stringResource(id = Lang.valueOfOrDefault(language).getRes()) ,
                    checked = language in successState.enabledLanguages,
                    onCheckedChanged = { onClickToggle(language) },
                )
            }
        }
    }
}