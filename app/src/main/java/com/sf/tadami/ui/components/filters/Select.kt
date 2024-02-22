package com.sf.tadami.ui.components.filters

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import com.sf.tadami.R
import com.sf.tadami.source.model.AnimeFilter
import com.sf.tadami.ui.utils.capFirstLetter

@Composable
fun Select(
    select: AnimeFilter.Select,
    onSelectUpdate : (select : AnimeFilter.Select) -> Unit
) {
    var expanded by remember {
        mutableStateOf(false)
    }

    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            text = select.name.capFirstLetter(),
            style = MaterialTheme.typography.labelLarge,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .clickable(
                    onClick = { expanded = true },
                    interactionSource = interactionSource,
                    indication = null
                ),
                contentAlignment = Alignment.CenterEnd
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
            ) {
                Text(
                    text = select.values[select.state],
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(weight = 1f, fill = false),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1
                )
                IconButton(
                    modifier = Modifier,
                    onClick = { expanded = true },
                    interactionSource = interactionSource
                )
                {
                    Icon(
                        painterResource(id = if(!expanded) R.drawable.ic_arrow_down else R.drawable.ic_arrow_up),
                        contentDescription = null
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                )
                {
                    select.values.forEach {
                        DropdownMenuItem(
                            text = { Text(text = it, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            onClick = {
                                select.state = select.values.indexOf(it)
                                onSelectUpdate(select)
                                expanded = false
                            }
                        )
                    }
                }
            }

        }
    }
}