package com.sf.tadami.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.sf.tadami.R
import com.sf.tadami.ui.utils.capFirstLetter
import com.sf.tadami.ui.utils.defaultParser

@Composable
fun ExposedIntDropdownMenu(
    modifier: Modifier = Modifier,
    initialStatus: Boolean = false,
    currentValue: Int,
    @StringRes name: Int,
    onChange: (item: Int) -> Unit,
    size: Int
) {

    var expanded by rememberSaveable { mutableStateOf(initialStatus) }
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(id = name).capFirstLetter(),
            style = MaterialTheme.typography.titleMedium,
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
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
                modifier = Modifier.fillMaxWidth().background(Color.Red)
            ) {
                Text(
                    text = currentValue.defaultParser(),
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
                    repeat(size) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = it.defaultParser(),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            onClick = {
                                onChange(it)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}