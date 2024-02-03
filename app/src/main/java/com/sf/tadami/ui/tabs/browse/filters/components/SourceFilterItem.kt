package com.sf.tadami.ui.tabs.browse.filters.components

import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sf.tadami.domain.source.Source
import com.sf.tadami.ui.tabs.browse.tabs.sources.components.item.BaseSourceItem

@Composable
fun SourcesFilterItem(
    source: Source,
    enabled: Boolean,
    onClickItem: (Source) -> Unit,
    modifier: Modifier = Modifier,
) {
    BaseSourceItem(
        modifier = modifier,
        source = source,
        onClickItem = { onClickItem(source) },
        action = {
            Checkbox(checked = enabled, onCheckedChange = null)
        },
    )
}