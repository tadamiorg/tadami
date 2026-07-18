package com.sf.tadami.ui.animeinfos.details.infos

import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sf.tadami.R
import com.sf.tadami.source.model.SAnimeStatus
import com.sf.tadami.ui.utils.padding
import com.sf.tadami.ui.utils.secondaryItemAlpha

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AnimeAndSourceTitles(
    appBarPadding: Dp,
    cover: () -> String,
    title: String,
    studio: String?,
    author: String?,
    release: String?,
    status: SAnimeStatus,
    sourceName : String,
    isStubSource : Boolean,
    onTitleClicked: () -> Unit = {}
) {
    val clipboardManager = LocalClipboardManager.current
    // Full-text reveal dialog for truncated metadata rows (studio / author / release).
    var revealed by remember { mutableStateOf<RevealedInfo?>(null) }
    revealed?.let { info ->
        AlertDialog(
            onDismissRequest = { revealed = null },
            title = { Text(text = stringResource(id = info.titleRes)) },
            confirmButton = {
                TextButton(onClick = { revealed = null }) {
                    Text(text = stringResource(id = android.R.string.ok))
                }
            },
            text = {
                Text(
                    text = info.text,
                    modifier = Modifier
                        .heightIn(max = 240.dp)
                        .verticalScroll(rememberScrollState()),
                )
            },
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = MaterialTheme.padding.medium, top = appBarPadding + MaterialTheme.padding.medium, end = MaterialTheme.padding.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimeCover.Book(
            modifier = Modifier
                .sizeIn(maxWidth = 100.dp)
                .align(Alignment.Top),
            data = cover(),
            contentDescription = "",
        )
        Column(modifier = Modifier.padding(start = MaterialTheme.padding.medium)) {
            Text(
                text = title.ifBlank { "" },
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.combinedClickable(
                    onClick = onTitleClicked,
                    onLongClick = {
                        if (title.isNotBlank()) {
                            // The OS shows its own copy confirmation, so no toast here.
                            clipboardManager.setText(AnnotatedString(title))
                        }
                    },
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            // Studio (primary metadata line)
            val studioText = studio?.takeIf { it.isNotBlank() } ?: stringResource(id = R.string.unknown_studio)
            IconTextRow(
                icon = Icons.Filled.Brush,
                text = studioText,
                onClick = { revealed = RevealedInfo(R.string.anime_info_studio, studioText) },
            )
            // Author (secondary metadata line)
            if (!author.isNullOrBlank() && author != studio) {
                IconTextRow(
                    icon = Icons.Filled.PersonOutline,
                    text = author,
                    onClick = { revealed = RevealedInfo(R.string.anime_info_author, author) },
                )
            }
            // Release date (no reveal dialog — plain row)
            if (!release.isNullOrBlank()) {
                IconTextRow(
                    icon = Icons.Outlined.CalendarMonth,
                    text = release,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.secondaryItemAlpha(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = status.icon(),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = MaterialTheme.padding.tiny)
                        .size(16.dp),
                )
                ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
                    Text(
                        text = stringResource(id = status.stringResId()),
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                    )
                    DotSeparatorText()
                    if (isStubSource) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(16.dp),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                    Text(
                        text = sourceName,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/** Data backing the reveal dialog: a localized field title + the full value to display. */
private data class RevealedInfo(@StringRes val titleRes: Int, val text: String)

/**
 * A metadata row (studio / author / release): a single ellipsized line. When [onClick] is provided
 * (studio / author), tapping it reveals the full value in a dialog handled by the caller; otherwise
 * (release) it's a plain, non-interactive row.
 */
@Composable
private fun IconTextRow(
    icon: ImageVector,
    text: String,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .secondaryItemAlpha()
            .padding(top = 2.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
        )
    }
}

fun SAnimeStatus.stringResId(): Int = when (this) {
    SAnimeStatus.ONGOING -> R.string.status_ongoing
    SAnimeStatus.COMPLETED -> R.string.status_completed
    SAnimeStatus.ON_HIATUS -> R.string.status_on_hiatus
    SAnimeStatus.CANCELLED -> R.string.status_cancelled
    SAnimeStatus.UNKNOWN -> R.string.status_unknown
}

fun SAnimeStatus.icon(): ImageVector = when (this) {
    SAnimeStatus.ONGOING -> Icons.Outlined.Schedule
    SAnimeStatus.COMPLETED -> Icons.Outlined.DoneAll
    SAnimeStatus.ON_HIATUS -> Icons.Outlined.Pause
    SAnimeStatus.CANCELLED -> Icons.Outlined.Close
    SAnimeStatus.UNKNOWN -> Icons.Outlined.Block
}

@Composable
fun DotSeparatorText() {
    Text(text = " • ")
}

@Composable
fun DotSeparatorNoSpaceText(
    modifier: Modifier = Modifier,
) {
    Text(
        text = "•",
        modifier = modifier,
    )
}
