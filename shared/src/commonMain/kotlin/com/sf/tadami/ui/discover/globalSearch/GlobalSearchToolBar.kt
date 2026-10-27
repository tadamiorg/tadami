package com.sf.tadami.ui.discover.globalSearch

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.sf.tadami.ui.components.data.Action
import com.sf.tadami.ui.components.topappbar.search.SearchTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSearchToolbar(
    onSearch: (value: String) -> Unit,
    onSearchChange: (value: String) -> Unit,
    onSearchCancel: () -> Unit,
    actions: List<Action> = emptyList(),
    searchValue: String,
    backHandlerEnabled : Boolean = false,
    searchOpened : Boolean = true,
    total : Int,
    progress : Int
) {

    Box{
        SearchTopAppBar(
            backHandlerEnabled = backHandlerEnabled,
            searchOpened = searchOpened,
            onSearch = onSearch,
            onSearchChange = onSearchChange,
            onSearchCancel = onSearchCancel,
            actions = actions,
            searchValue = searchValue
        )
        if (progress in 0 until total) {
            LinearProgressIndicator(
                progress = { progress / total.toFloat() },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(),
            )
        }
    }

}