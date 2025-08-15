package com.sf.tadami.ui.animeinfos.details.infos.description

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sf.tadami.R
import com.sf.tadami.ui.utils.clickableNoIndication
import com.sf.tadami.ui.utils.padding

private val whitespaceLineRegex = Regex("[\\r\\n]{2,}", setOf(RegexOption.MULTILINE))
private val defaultTagChipModifier = Modifier.padding(vertical = 4.dp)

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
                    .padding(top = 8.dp)
                    .padding(vertical = 12.dp)
                    .animateContentSize(animationSpec = spring())
                    .fillMaxWidth(),
            ) {
                FlowRow(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
                ) {
                    tags.forEach {
                        TagsChip(
                            modifier = defaultTagChipModifier,
                            text = it,
                            onClick = {
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TagsChip(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
        SuggestionChip(
            modifier = modifier,
            onClick = onClick,
            label = { Text(text = text, style = MaterialTheme.typography.bodySmall) },
        )
    }
}
