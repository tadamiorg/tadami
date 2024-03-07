package com.sf.tadami.ui.animeinfos.details.actions

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Public
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import com.sf.tadami.R


@Composable()
fun AnimeActionRow(
    modifier: Modifier = Modifier,
    favorite: Boolean? = false,
    onAddToLibraryClicked: () -> Unit,
    onWebViewClicked: (() -> Unit)?,
    onWebViewLongClicked: (() -> Unit)?,
) {
    val defaultActionButtonColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .38f)

    Row(modifier = modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp)) {
        AnimeActionButton(
            title = if (favorite==true) {
                stringResource(R.string.in_library)
            } else {
                stringResource(R.string.add_to_library)
            },
            icon = if (favorite==true) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            color = if (favorite==true) MaterialTheme.colorScheme.primary else defaultActionButtonColor,
            onClick = onAddToLibraryClicked,
        )
        if (onWebViewClicked != null) {
            AnimeActionButton(
                title = stringResource(R.string.action_web_view),
                icon = Icons.Outlined.Public,
                color = defaultActionButtonColor,
                onClick = onWebViewClicked,
                onLongClick = onWebViewLongClicked,
            )
        }
    }
}