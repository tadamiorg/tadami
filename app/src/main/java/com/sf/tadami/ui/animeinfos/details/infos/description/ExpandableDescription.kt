package com.sf.tadami.ui.animeinfos.details.infos.description

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.sf.tadami.R
import com.sf.tadami.ui.utils.clickableNoIndication
import com.sf.tadami.ui.utils.padding

private val whitespaceLineRegex = Regex("[\\r\\n]{2,}", setOf(RegexOption.MULTILINE))

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpandableAnimeDescription(
    modifier: Modifier = Modifier,
    defaultExpandState: Boolean,
    description: String?,
    tagsProvider: () -> List<String>?,
) {
    Column(modifier = modifier) {
        val (expanded, onExpanded) = rememberSaveable {
            mutableStateOf(defaultExpandState)
        }
        val desc = description.takeIf { !it.isNullOrBlank() } ?: stringResource(id = R.string.unknown_description)
        val trimmedDescription = remember(desc) {
            desc
                .replace(whitespaceLineRegex, "\n")
                .trimEnd()
        }
        AnimeDescription(
            expandedDescription = desc,
            shrunkDescription = trimmedDescription,
            expanded = expanded,
            modifier = Modifier
                .padding(top = MaterialTheme.padding.extraSmall)
                .padding(horizontal = MaterialTheme.padding.medium)
                .clickableNoIndication(onClick = { onExpanded(!expanded) }),
        )
        val tags = tagsProvider()
        if (!tags.isNullOrEmpty()) {
            Box(
                modifier = Modifier
                    .padding(top = MaterialTheme.padding.extraSmall)
                    .padding(vertical = MaterialTheme.padding.small)
                    .animateContentSize(),
            ) {
                FlowRow(
                    modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall)
                ) {
                    tags.forEach {
                        TagsChip(
                            text = it,
                            onClick = {  },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagsChip(
    text: String,
    onClick: () -> Unit,
) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
        SuggestionChip(
            onClick = onClick,
            label = { Text(text = text, style = MaterialTheme.typography.bodySmall) },
        )
    }
}
