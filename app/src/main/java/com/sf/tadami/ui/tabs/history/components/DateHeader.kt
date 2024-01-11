package com.sf.tadami.ui.tabs.history.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.sf.tadami.ui.utils.padding
import com.sf.tadami.ui.utils.toRelativeString
import java.util.Date

@Composable
fun DateHeader(
    modifier: Modifier = Modifier,
    date: Date,
) {
    val context = LocalContext.current
    Text(
        text = remember {
            date.toRelativeString(context = context)
        },
        modifier = modifier
            .padding(
                horizontal = MaterialTheme.padding.medium,
                vertical = MaterialTheme.padding.small,
            ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
        style = MaterialTheme.typography.bodyMedium,
    )
}