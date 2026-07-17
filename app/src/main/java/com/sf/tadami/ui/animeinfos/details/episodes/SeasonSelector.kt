package com.sf.tadami.ui.animeinfos.details.episodes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.sf.tadami.source.model.SSeason
import com.sf.tadami.ui.utils.padding

@Composable
fun SeasonSelector(
    modifier: Modifier = Modifier,
    seasons: List<SSeason>,
    selectedSeason: SSeason?,
    onSeasonSelected: (SSeason) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .padding(
                    horizontal = MaterialTheme.padding.medium,
                    vertical = MaterialTheme.padding.tiny
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selectedSeason?.name ?: "",
                style = MaterialTheme.typography.titleMedium
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            seasons.forEach { season ->
                DropdownMenuItem(
                    text = { Text(season.name) },
                    onClick = {
                        expanded = false
                        onSeasonSelected(season)
                    }
                )
            }
        }
    }
}

/**
 * Static season label for anime whose seasons are split into separate anime entries
 * (no in-app selector). Displayed in the same place as [SeasonSelector].
 */
@Composable
fun SeasonLabel(
    modifier: Modifier = Modifier,
    name: String
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = MaterialTheme.padding.medium,
                vertical = MaterialTheme.padding.tiny
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleMedium
        )
    }
}
