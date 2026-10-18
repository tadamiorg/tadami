package com.sf.tadami.ui.animeinfos.details

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sf.tadami.R
import com.sf.tadami.ui.animeinfos.details.episodes.DetailsDownloadAction
import com.sf.tadami.ui.components.data.Action
import com.sf.tadami.ui.components.data.DropDownAction
import com.sf.tadami.ui.components.topappbar.ContextualTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsToolbar(
    modifier: Modifier = Modifier,
    title: String,
    onBackClicked: () -> Unit,
    onMigrateClicked : () -> Unit,
    episodesListState: LazyListState,
    migrationEnabled : Boolean = false,
    // In the library only
    onEditCategoriesClicked : (() -> Unit)? = null,
    onDownloadClicked : (DetailsDownloadAction) -> Unit,
    onDeleteDownloadsClicked : () -> Unit,
    hasDownloads : Boolean,
    // Only downloaded episodes are listed: nothing left to download
    downloadedOnly : Boolean,
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
        if (firstVisibleItemIndex > 0) 1f else 0f, label = "title_alpha",
    )
    val animatedBgAlpha by animateFloatAsState(
        if (firstVisibleItemIndex > 0 || firstVisibleItemScrollOffset > 0) 1f else 0f,
        label = "bg_alpha",
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
            Action.CastButton(),
            Action.DropDownVector(
                title = R.string.download,
                icon = Icons.Outlined.Download,
                onClick = {},
                items = listOf(
                    DetailsDownloadAction.NEXT_1,
                    DetailsDownloadAction.NEXT_5,
                    DetailsDownloadAction.NEXT_10,
                ).map { action ->
                    DropDownAction(
                        title = pluralStringResource(R.plurals.download_next_unseen, action.count!!, action.count),
                        onClick = { onDownloadClicked(action) },
                        enabled = !downloadedOnly,
                    )
                } + listOf(
                    DropDownAction(
                        title = stringResource(id = R.string.download_unseen),
                        onClick = { onDownloadClicked(DetailsDownloadAction.UNSEEN) },
                        enabled = !downloadedOnly,
                    ),
                    DropDownAction(
                        title = stringResource(id = R.string.download_all),
                        onClick = { onDownloadClicked(DetailsDownloadAction.ALL) },
                        enabled = !downloadedOnly,
                    ),
                    DropDownAction(
                        title = stringResource(id = R.string.download_delete_all),
                        onClick = onDeleteDownloadsClicked,
                        enabled = hasDownloads,
                    ),
                )
            ),
            Action.DropDownDrawable(
                title = R.string.stub_text,
                icon = R.drawable.ic_vertical_settings,
                items = listOfNotNull(
                    onEditCategoriesClicked?.let {
                        DropDownAction(
                            title = stringResource(id = R.string.action_edit_categories),
                            onClick = it
                        )
                    },
                    DropDownAction(
                        title = stringResource(id = R.string.action_migrate),
                        onClick = onMigrateClicked,
                        enabled = migrationEnabled
                    )
                )
            )
        ),
        navigationIcon = {
            IconButton(onClick = onBackClicked) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = null,
                )
            }
        },
        colors = topAppBarColors(
            containerColor = MaterialTheme.colorScheme
                .surfaceColorAtElevation(3.dp)
                .copy(alpha = animatedBgAlpha),
        ),
        actionModeCounter = actionModeCounter,
        onCloseActionModeClicked = onCloseClicked,
        onToggleAll = onToggleAll,
        onInverseAll = onInverseAll
    )

}

