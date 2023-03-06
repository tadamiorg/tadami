package com.sf.animescraper.ui.base.widgets.topbar

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sf.animescraper.R
import com.sf.animescraper.ui.components.toolbar.Action


@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenTopBar(
    modifier: Modifier = Modifier,
    title: String,
    titleStyle: TextStyle? = null,
    backArrow: Boolean = false,
    backArrowAction: () -> Unit = {},
    immersive: Boolean = false,
    actions: List<Action>? = null,
    alpha: Float = 1f,
    content: @Composable (topPadding: PaddingValues) -> Unit
) {
    Scaffold(topBar = {
        TopAppBar(
            modifier = modifier,
            title = {
                Text(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = titleStyle ?: MaterialTheme.typography.headlineSmall,
                    color = if (alpha==0f) Color.Transparent else MaterialTheme.colorScheme.onBackground
                )
            },
            navigationIcon = {
                if (backArrow) {
                    IconButton(onClick = backArrowAction) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back_arrow),
                            contentDescription = null
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.smallTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.background.copy(
                    alpha = alpha
                ),
                titleContentColor = if (alpha==0f) Color.Transparent else Color.Transparent,
            ),
            actions = {
                actions?.forEach { action ->
                    ActionItem(action)
                }
            }
        )
    }) { topPadding ->
        var padding = topPadding
        if (immersive) padding = PaddingValues(0.dp)
        Box(modifier = Modifier.padding(padding)) {
            content(topPadding)
        }
    }
}

