package com.sf.tadami.ui.discover.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sf.tadami.R
import com.sf.tadami.network.api.model.AnimeFilter
import com.sf.tadami.network.api.model.AnimeFilterList
import com.sf.tadami.ui.components.dialog.sheets.BottomSheet
import com.sf.tadami.ui.components.filters.CheckBox
import com.sf.tadami.ui.components.filters.Group
import com.sf.tadami.ui.components.filters.Select
import com.sf.tadami.ui.utils.padding

@Composable
fun FiltersSheet(
    filters: AnimeFilterList,
    onUpdateFilters: (filters: AnimeFilterList) -> Unit = {},
    onResetClicked: () -> Unit,
    search: () -> Unit
) {
    BottomSheet(
        header = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.padding.medium, vertical = MaterialTheme.padding.tiny),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onResetClicked) {
                    Text(text = stringResource(id = R.string.action_reset))
                }
                Button(onClick = search) {
                    Text(text = stringResource(id = R.string.action_filter))
                }
            }

            HorizontalDivider(thickness = 1.dp)
        }
    ){
        filters.forEachIndexed { index, animeFilter ->
            when (animeFilter) {
                is AnimeFilter.Select -> {
                    Select(
                        select = animeFilter,
                        onSelectUpdate = {
                            filters[index] = it
                            onUpdateFilters(filters)
                        }
                    )
                }
                is AnimeFilter.CheckBoxGroup -> {
                    Group(
                        checkBoxGroup = animeFilter,
                        onUpdateGroup = {
                            filters[index] = it
                            onUpdateFilters(filters)
                        }
                    )
                }
                is AnimeFilter.Header -> {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                vertical = MaterialTheme.padding.extraSmall
                            ),
                        text = animeFilter.name,
                        maxLines = 1,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                is AnimeFilter.CheckBox -> {
                    CheckBox(
                        title = animeFilter.name,
                        state = animeFilter.state,
                        onCheckedChange = {
                            (filters[index] as AnimeFilter.CheckBox).state = it
                            onUpdateFilters(filters)
                        }
                    )
                }
            }
        }
    }
}