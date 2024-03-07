package com.sf.tadami.ui.components.dialog.alert

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import com.sf.tadami.ui.components.dialog.alert.AlertDialogConstants.DialogHorizontalPadding
import com.sf.tadami.ui.components.dialog.alert.AlertDialogConstants.DialogTitlePadding

@Composable
internal fun AlertDialogContent(
    modifier: Modifier = Modifier,
    dialogHorizontalPadding : Boolean = true,
    buttons: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable BoxScope.() -> Unit,
    shape: Shape,
    containerColor: Color,
    tonalElevation: Dp,
    buttonContentColor: Color,
    titleContentColor: Color,
    textContentColor: Color,
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = containerColor,
        tonalElevation = tonalElevation,
    ) {
        Column(
            modifier = Modifier.padding(if(dialogHorizontalPadding) DialogHorizontalPadding else PaddingValues(0.dp))
        ) {
            title?.let{
                ProvideContentColorTextStyle(
                    contentColor = titleContentColor,
                    textStyle = MaterialTheme.typography.titleLarge
                ) {
                    Box(
                        // Align the title to the center when an icon is present.
                        Modifier
                            .padding(DialogTitlePadding)
                            .align(Alignment.Start)
                    ) {
                        it()
                    }
                }
            }

            ProvideContentColorTextStyle(
                contentColor = textContentColor,
                textStyle = MaterialTheme.typography.bodyMedium
            ) {
                Box(
                    Modifier
                        .weight(weight = 1f, fill = false)
                        .align(Alignment.Start)
                ) {
                    text()
                }
            }

            buttons?.let {
                Box(modifier = Modifier.align(Alignment.End)) {
                    ProvideContentColorTextStyle(
                        contentColor = buttonContentColor,
                        textStyle = MaterialTheme.typography.labelLarge,
                        content = it
                    )
                }
            }
        }
    }
}

@Composable
internal fun ProvideContentColorTextStyle(
    contentColor: Color,
    textStyle: TextStyle,
    content: @Composable () -> Unit
) {
    val mergedStyle = LocalTextStyle.current.merge(textStyle)
    CompositionLocalProvider(
        LocalContentColor provides contentColor,
        LocalTextStyle provides mergedStyle,
        content = content
    )
}
