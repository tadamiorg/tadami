package com.sf.tadami.ui.components.dialog.simple

import android.content.res.Configuration
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.booleanResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.sf.tadami.R
import com.sf.tadami.ui.components.dialog.alert.AlertDialogConstants.ButtonsCrossAxisSpacing
import com.sf.tadami.ui.components.dialog.alert.AlertDialogConstants.ButtonsMainAxisSpacing
import com.sf.tadami.ui.components.dialog.alert.AlertDialogContent
import com.sf.tadami.ui.components.dialog.alert.AlertDialogFlowRow

@Composable
fun SimpleDialog(
    modifier: Modifier = Modifier,
    opened : Boolean,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable (() -> Unit)? = null,
    title: @Composable () -> Unit = {},
    shape: Shape = MaterialTheme.shapes.small,
    containerColor: Color = AlertDialogDefaults.containerColor,
    titleContentColor: Color = AlertDialogDefaults.titleContentColor,
    textContentColor: Color = AlertDialogDefaults.textContentColor,
    tonalElevation: Dp = AlertDialogDefaults.TonalElevation,
    onDismissRequest: () -> Unit,
    text: @Composable ColumnScope.() -> Unit = {},
) {
    val configuration = LocalConfiguration.current


    val isTablet = booleanResource(id = R.bool.is_tablet)

    val widthFraction = remember {
        when (configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> if (isTablet) 0.3f else 0.45f
            else -> if (isTablet) 0.45f else 0.9f
        }
    }
    val maxWidth = remember {
        derivedStateOf {
            configuration.screenWidthDp.dp * widthFraction
        }
    }
    Crossfade(modifier = Modifier.zIndex(5f),targetState = opened, label = "") { visible ->
        if (visible) {
            Box(
                contentAlignment = Alignment.Center
            )
            {
                Surface(modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onDismissRequest()
                    },
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.6f)
                ) {}
                AlertDialogContent(
                    modifier = modifier
                        .requiredWidthIn(max = maxWidth.value)
                        .wrapContentWidth(),
                    buttons = {
                        AlertDialogFlowRow(
                            mainAxisSpacing = ButtonsMainAxisSpacing,
                            crossAxisSpacing = ButtonsCrossAxisSpacing
                        ) {
                            dismissButton?.invoke()
                            confirmButton()
                        }
                    },
                    title = title,
                    text = text,
                    shape = shape,
                    containerColor = containerColor,
                    tonalElevation = tonalElevation,
                    buttonContentColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = titleContentColor,
                    textContentColor = textContentColor,
                )
            }
        }
    }
}