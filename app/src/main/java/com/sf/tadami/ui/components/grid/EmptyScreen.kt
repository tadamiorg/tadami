package com.sf.tadami.ui.components.grid

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.sf.tadami.ui.utils.padding

@Composable
fun EmptyScreen(
    modifier: Modifier = Modifier,
    actions: List<EmptyScreenAction> = emptyList(),
    message: String
) {

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = MaterialTheme.padding.large),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = message, style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier
                .padding(
                    top = MaterialTheme.padding.large,
                    start = MaterialTheme.padding.large,
                    end = MaterialTheme.padding.large,
                ),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium)
        ) {
            actions.forEach { action ->
                Action(
                    modifier = Modifier.weight(1f, false),
                    icon = action.icon,
                    title = stringResource(id = action.stringResId),
                    onClick = action.onClick
                )
            }
        }
    }
}

@Composable
private fun Action(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Button(
        modifier = modifier,
        onClick = onClick
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = title,
                textAlign = TextAlign.Center,
            )
        }
    }
}

data class EmptyScreenAction(
    @StringRes val stringResId: Int,
    val icon: ImageVector,
    val onClick: () -> Unit,
)