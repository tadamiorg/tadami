package com.sf.tadami.ui.tabs.settings.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.ArrowDropUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.sf.tadami.ui.utils.padding

@Composable
fun ReorderListItem(
    item: Pair<String, String?>,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: (Pair<String, String>) -> Unit,
    onMoveDown: (Pair<String, String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (item.second != null) {
        OutlinedCard (
            modifier = modifier
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = MaterialTheme.padding.medium,
                        end = MaterialTheme.padding.medium,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Text(
                    text = item.second!!
                )
                Row {
                    IconButton(
                        onClick = { onMoveUp(item as Pair<String, String>) },
                        enabled = canMoveUp,
                    ) {
                        Icon(imageVector = Icons.Outlined.ArrowDropUp, contentDescription = null)
                    }
                    IconButton(
                        onClick = { onMoveDown(item as Pair<String, String>) },
                        enabled = canMoveDown,
                    ) {
                        Icon(imageVector = Icons.Outlined.ArrowDropDown, contentDescription = null)
                    }
                }
            }
        }
    }
}
