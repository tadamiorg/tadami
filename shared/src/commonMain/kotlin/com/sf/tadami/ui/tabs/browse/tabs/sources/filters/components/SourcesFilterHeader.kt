package com.sf.tadami.ui.tabs.browse.tabs.sources.filters.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import com.sf.tadami.ui.tabs.more.settings.widget.TogglePreference
import com.sf.tadami.utils.Lang

@Composable
fun SourcesFilterHeader(
    language: Lang,
    enabled: Boolean,
    onClickItem: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    TogglePreference(
        modifier = modifier,
        title = stringResource(language.res),
        checked = enabled,
        onCheckedChanged = { onClickItem(language.name) },
    )
}