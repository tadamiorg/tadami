package com.sf.tadami.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun BoxScope.AnimeItemBadge(
    modifier: Modifier = Modifier,
    text : String
) {
    Row(
        modifier = modifier
            .padding(4.dp)
            .align(Alignment.TopStart)
            .clip(MaterialTheme.shapes.small)
    ){
        Text(
            text = text,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.secondary)
                .padding(horizontal = 3.dp, vertical = 1.dp),
            color = MaterialTheme.colorScheme.onSecondary,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}