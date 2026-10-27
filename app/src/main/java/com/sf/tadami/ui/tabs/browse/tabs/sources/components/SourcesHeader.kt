package com.sf.tadami.ui.tabs.browse.tabs.sources.components

import org.jetbrains.compose.resources.StringResource
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.sf.tadami.ui.utils.padding

@Composable
fun SourcesHeader(
    language: StringResource,
    modifier: Modifier = Modifier,
) {
    Text(
        text = stringResource(language),
        modifier = modifier
            .padding(
                horizontal = MaterialTheme.padding.medium,
                vertical = MaterialTheme.padding.small
            ),
        style = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        ),
    )
}