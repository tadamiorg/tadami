package com.sf.tadami.ui.main.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.sf.tadami.ui.utils.padding
import com.sf.tadami.ui.utils.secondaryItemAlpha

@Composable
fun InfoScreen(
    icon: ImageVector,
    headingText: String,
    subtitleText: String,
    acceptText: String,
    onAcceptClick: () -> Unit,
    canAccept: Boolean = true,
    rejectText: String? = null,
    onRejectClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        bottomBar = {
            val strokeWidth = Dp.Hairline
            val borderColor = MaterialTheme.colorScheme.outline
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .drawBehind {
                        drawLine(
                            borderColor,
                            Offset(0f, 0f),
                            Offset(size.width, 0f),
                            strokeWidth.value,
                        )
                    }
                    .windowInsetsPadding(NavigationBarDefaults.windowInsets)
                    .padding(
                        horizontal = MaterialTheme.padding.medium,
                        vertical = MaterialTheme.padding.small,
                    ),
            ) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = canAccept,
                    onClick = onAcceptClick,
                ) {
                    Text(text = acceptText)
                }
                if (rejectText != null && onRejectClick != null) {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onRejectClick,
                    ) {
                        Text(text = rejectText)
                    }
                }
            }
        },
    ) { paddingValues ->
        // Status bar scrim
        Box(
            modifier = Modifier
                .zIndex(2f)
                .secondaryItemAlpha()
                .background(MaterialTheme.colorScheme.background)
                .fillMaxWidth()
                .height(paddingValues.calculateTopPadding()),
        )

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxWidth()
                .padding(paddingValues)
                .padding(top = 48.dp)
                .padding(horizontal = MaterialTheme.padding.medium),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .padding(bottom = MaterialTheme.padding.small)
                    .size(48.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = headingText,
                style = MaterialTheme.typography.headlineLarge,
            )
            Text(
                text = subtitleText,
                modifier = Modifier
                    .secondaryItemAlpha()
                    .padding(vertical = MaterialTheme.padding.small),
                style = MaterialTheme.typography.titleSmall,
            )

            content()
        }
    }
}