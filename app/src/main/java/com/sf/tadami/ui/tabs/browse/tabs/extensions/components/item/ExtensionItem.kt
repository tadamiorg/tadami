package com.sf.tadami.ui.tabs.browse.tabs.extensions.components.item

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sf.tadami.domain.extensions.Extension
import com.sf.tadami.ui.tabs.browse.tabs.extensions.ExtensionUiModel
import com.sf.tadami.ui.tabs.browse.tabs.extensions.components.ExtensionIcon

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExtensionItem(
    item: ExtensionUiModel.Item,
    onClickItem: (Extension) -> Unit,
    onLongClickItem: (Extension) -> Unit,
    onClickItemCancel: (Extension) -> Unit,
    onClickItemAction: (Extension) -> Unit,
    onClickItemSecondaryAction: (Extension) -> Unit,
    modifier: Modifier = Modifier,
) {
    val (extension, installStep) = item
    BaseBrowseItem(
        modifier = modifier
            .combinedClickable(
                onClick = { onClickItem(extension) },
                onLongClick = { onLongClickItem(extension) },
            ),
        onClickItem = { onClickItem(extension) },
        onLongClickItem = { onLongClickItem(extension) },
        icon = {
            Box(
                modifier = Modifier
                    .size(40.dp),
                contentAlignment = Alignment.Center,
            ) {
                val idle = installStep.isCompleted()
                if (!idle) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(40.dp),
                        strokeWidth = 2.dp,
                    )
                }

                val padding by animateDpAsState(
                    targetValue = if (idle) 0.dp else 8.dp,
                    label = "iconPadding",
                )
                ExtensionIcon(
                    extension = extension,
                    modifier = Modifier
                        .matchParentSize()
                        .padding(padding),
                )
            }
        },
        action = {
            ExtensionItemActions(
                extension = extension,
                installStep = installStep,
                onClickItemCancel = onClickItemCancel,
                onClickItemAction = onClickItemAction,
                onClickItemSecondaryAction = onClickItemSecondaryAction,
            )
        },
    ) {
        ExtensionItemContent(
            extension = extension,
            installStep = installStep,
            modifier = Modifier.weight(1f),
        )
    }
}