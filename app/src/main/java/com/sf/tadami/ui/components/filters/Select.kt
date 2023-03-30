package com.sf.tadami.ui.components.filters

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import com.sf.tadami.network.api.model.AnimeFilter
import com.sf.tadami.ui.utils.capFirstLetter
import com.sf.tadami.R

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
                .pointerInput(interactionSource) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        expanded = true
                    }
                }
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
                    modifier = Modifier
                        .pointerInput(interactionSource) {
                            detectDragGestures { change, _ ->
                                change.consume()
                                expanded = true
                            }
                        },
                    onClick = { expanded = true },
                    interactionSource = interactionSource
                )
                {
                    Icon(
                        painterResource(id = R.drawable.ic_arrow_down),
                        contentDescription = null
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
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