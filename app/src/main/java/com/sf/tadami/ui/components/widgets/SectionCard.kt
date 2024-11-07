package com.sf.tadami.ui.components.widgets

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.sf.tadami.ui.utils.padding

@Composable
fun LazyItemScope.SectionCard(
    @StringRes titleRes: Int? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (titleRes != null) {
        Text(
            modifier = Modifier.padding(horizontal = MaterialTheme.padding.extraLarge),
            text = stringResource(titleRes),
            style = MaterialTheme.typography.titleSmall,
        )
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = MaterialTheme.padding.medium,
                vertical = MaterialTheme.padding.small,
            ),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.padding.medium)) {
            content()
        }
    }
}
