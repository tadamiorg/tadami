package com.sf.animescraper.ui.animeinfos.details

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
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
import com.sf.animescraper.ui.components.toolbar.Action
import com.sf.animescraper.ui.components.toolbar.ContextualTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsToolbar(
    modifier: Modifier = Modifier,
    title: String,
    onBackClicked: () -> Unit,
    onFavoriteClicked: () -> Unit,
    episodesListState: LazyListState,
    isFavorited : Boolean? = false,
    // For Action Mode
    actionModeCounter: Int,
    onCloseClicked: () -> Unit,
    onToggleAll: () -> Unit,
    onInverseAll: () -> Unit
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
    ContextualTopAppBar(
        modifier = modifier,
        title = {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.alpha(
                    animatedTitleAlpha
                ),
            )
        },
        actions = listOf(
            Action.Vector(
                title = androidx.appcompat.R.string.search_menu_title,
                icon = if(isFavorited == true) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                onClick = onFavoriteClicked
            ),
            Action.Drawable(
                title = androidx.appcompat.R.string.search_menu_title,
                icon = R.drawable.ic_vertical_settings,
                onClick = {},
                enabled = false
            )
        ),
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
                .copy(alpha = animatedBgAlpha),
        ),
        actionModeCounter = actionModeCounter,
        onCloseClicked = onCloseClicked,
        onToggleAll = onToggleAll,
        onInverseAll = onInverseAll
    )

}

