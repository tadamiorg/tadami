package com.sf.animescraper.ui.animeinfos.details

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sf.animescraper.R
import com.sf.animescraper.ui.base.widgets.topbar.ActionItem
import com.sf.animescraper.ui.components.toolbar.Action

@Composable
fun StateDetailsToolbar(
    modifier: Modifier = Modifier,
    title: String,
    onBackClicked: () -> Unit,
    onFavoriteClicked: () -> Unit,
    episodesListState: LazyListState,
) {
    val firstVisibleItemIndex by remember {
        derivedStateOf { episodesListState.firstVisibleItemIndex }
    }
    val firstVisibleItemScrollOffset by remember {
        derivedStateOf { episodesListState.firstVisibleItemScrollOffset }
    }
    val animatedTitleAlpha by animateFloatAsState(
        if (firstVisibleItemIndex > 0) 1f else 0f,
    )
    val animatedBgAlpha by animateFloatAsState(
        if (firstVisibleItemIndex > 0 || firstVisibleItemScrollOffset > 0) 1f else 0f,
    )
    DetailsToolbar(modifier,title,{animatedTitleAlpha},{animatedBgAlpha},onFavoriteClicked,onBackClicked)
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailsToolbar(
    modifier: Modifier = Modifier,
    title: String,
    titleAlphaProvider: () -> Float,
    backgroundAlphaProvider: () -> Float = titleAlphaProvider,
    onFavoriteClicked: () -> Unit,
    onBackClicked: () -> Unit,
) {
    val actions = listOf(
        Action(title = androidx.appcompat.R.string.search_menu_title, icon = R.drawable.ic_favorite, onClick = onFavoriteClicked),
        Action(title = androidx.appcompat.R.string.search_menu_title, icon = R.drawable.ic_vertical_settings, onClick = {})
    )

    Column(
        modifier = modifier,
    ) {
        TopAppBar(
            title = {
                Text(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.alpha(titleAlphaProvider()),
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClicked) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowBack,
                        contentDescription = null,
                    )
                }
            },
            colors = TopAppBarDefaults.smallTopAppBarColors(
                containerColor = MaterialTheme.colorScheme
                    .surfaceColorAtElevation(3.dp)
                    .copy(alpha = backgroundAlphaProvider()),
            ),
            actions = {
                actions.forEach {action ->
                    ActionItem(action = action)
                }

            }
        )
    }
}

