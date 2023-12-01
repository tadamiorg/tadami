package com.sf.tadami.ui.tabs.animesources.filters.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.sf.tadami.ui.tabs.settings.widget.TogglePreference

@Composable
fun SourcesFilterHeader(
    language: Int,
    enabled: Boolean,
    onClickItem: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    TogglePreference(
        modifier = modifier,
        title = stringResource(id = language),
        checked = enabled,
        onCheckedChanged = { onClickItem(language) },
    )
}