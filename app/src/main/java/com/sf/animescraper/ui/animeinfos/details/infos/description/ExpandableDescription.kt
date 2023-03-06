package com.sf.animescraper.ui.animeinfos.details.infos.description

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
import androidx.compose.ui.unit.dp
import com.google.accompanist.flowlayout.FlowRow
import com.sf.animescraper.R
import com.sf.animescraper.ui.utils.clickableNoIndication

private val whitespaceLineRegex = Regex("[\\r\\n]{2,}", setOf(RegexOption.MULTILINE))

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
                .padding(top = 8.dp)
                .padding(horizontal = 16.dp)
                .clickableNoIndication(onClick = { onExpanded(!expanded) }),
        )
        val tags = tagsProvider()
        if (!tags.isNullOrEmpty()) {
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .padding(vertical = 12.dp)
                    .animateContentSize(),
            ) {
                FlowRow(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    mainAxisSpacing = 4.dp,
                    crossAxisSpacing = 8.dp,
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
    CompositionLocalProvider(LocalMinimumTouchTargetEnforcement provides false) {
        SuggestionChip(
            onClick = onClick,
            label = { Text(text = text, style = MaterialTheme.typography.bodySmall) },
            border = null,
            colors = SuggestionChipDefaults.suggestionChipColors(
                containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                labelColor = MaterialTheme.colorScheme.onSurface,
            ),
        )
    }
}
