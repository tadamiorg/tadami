package com.sf.animescraper.ui.discover.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sf.animescraper.network.api.model.AnimeFilter
import com.sf.animescraper.network.api.model.AnimeFilterList
import com.sf.animescraper.ui.components.filters.Group
import com.sf.animescraper.ui.components.filters.Select

@Composable
fun FiltersSheet(
    hideSheet: () -> Unit,
    isVisible: Boolean = false,
    filters : AnimeFilterList,
    onUpdateFilters : (filters : AnimeFilterList) -> Unit = {},
    onResetClicked : () -> Unit,
    search : () -> Unit
) {
    val scrollState = rememberScrollState()

    if (isVisible) {
        BackHandler {
            hideSheet()
        }
    }

    // Filter sheet header

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TextButton(onClick = onResetClicked) {
            Text(text = "Reset")
        }
        Button(onClick = search) {
            Text(text = "filter")
        }
    }

    Divider(thickness = 1.dp)

    // Filter sheet content

    Column(
        modifier = Modifier
            .verticalScroll(scrollState, true)
            .padding(8.dp)
    ) {
        filters.forEachIndexed { index,animeFilter ->
            when(animeFilter){
                is AnimeFilter.Select -> {
                    Select(select = animeFilter, onSelectUpdate = {
                        filters[index] = it
                        onUpdateFilters(filters)
                    })
                }
                is AnimeFilter.CheckBoxGroup -> {
                    Group(checkBoxGroup = animeFilter, onUpdateGroup = {
                        filters[index] = it
                        onUpdateFilters(filters)
                    })
                }
                is AnimeFilter.Header -> {

                }
                is AnimeFilter.CheckBox -> {

                }
            }
        }

    }
}