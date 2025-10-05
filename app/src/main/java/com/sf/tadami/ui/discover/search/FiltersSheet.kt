package com.sf.tadami.ui.discover.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sf.tadami.R
import com.sf.tadami.source.model.AnimeFilter
import com.sf.tadami.source.model.AnimeFilterList
import com.sf.tadami.ui.components.filters.CheckboxItem
import com.sf.tadami.ui.components.filters.Group
import com.sf.tadami.ui.components.filters.SelectItem
import com.sf.tadami.ui.utils.capFirstLetter
import com.sf.tadami.ui.utils.padding

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiltersSheet(

    filters: AnimeFilterList,
    onUpdateFilters: (filters: AnimeFilterList) -> Unit = {},
    onResetClicked: () -> Unit,
    search: () -> Unit
) {

    LazyColumn {
        stickyHeader {
            Row(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .padding(8.dp),
            ) {
                TextButton(onClick = onResetClicked) {
                    Text(
                        text = stringResource(id = R.string.action_reset),
                        style = LocalTextStyle.current.copy(
                            color = MaterialTheme.colorScheme.primary,
                        ),
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(onClick = {
                    search()
                }) {
                    Text(stringResource(R.string.action_filter))
                }
            }
            HorizontalDivider()
        }

        // Filter items
        itemsIndexed(
            items = filters,
            key = { index, animeFilter -> animeFilter.name + index } // More stable key
        ) { index, animeFilter ->


            when (animeFilter) {
                is AnimeFilter.Select -> {
                    SelectItem(
                        label = animeFilter.name.capFirstLetter(),
                        options = animeFilter.values,
                        selectedIndex = animeFilter.state,
                        onSelect = {
                            val newList = filters.toMutableList()
                            (newList[index] as AnimeFilter.Select).state = it
                            onUpdateFilters(AnimeFilterList(newList))
                        }
                    )
                }

                is AnimeFilter.CheckBoxGroup -> {
                    Group(
                        checkBoxGroup = animeFilter,
                        onUpdateGroup = {
                            val newList = filters.toMutableList().apply { set(index, it) }
                            onUpdateFilters(AnimeFilterList(newList))
                        }
                    )
                }

                is AnimeFilter.Header -> {
                    if (animeFilter.name.isNotEmpty()) {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(color = Color.Red)
                                .padding(
                                    vertical = MaterialTheme.padding.extraSmall
                                ),
                            text = animeFilter.name,
                            maxLines = 1,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    } else {
                        HorizontalDivider(
                            modifier = Modifier.padding(
                                vertical = MaterialTheme.padding.extraSmall
                            ),
                            thickness = 2.dp
                        )
                    }
                }

                is AnimeFilter.CheckBox -> {
                    CheckboxItem(
                        label = animeFilter.name,
                        checked = animeFilter.state,
                    ) {
                        val newList = filters.toMutableList()
                        val curr = (newList[index] as AnimeFilter.CheckBox).state
                        (newList[index] as AnimeFilter.CheckBox).state = !curr
                        onUpdateFilters(AnimeFilterList(newList))
                    }
                }
            }
        }
    }
}


