package com.sf.tadami.ui.components.dialog.simple

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.booleanResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.sf.tadami.R
import com.sf.tadami.ui.components.dialog.alert.AlertDialogConstants.ButtonsCrossAxisSpacing
import com.sf.tadami.ui.components.dialog.alert.AlertDialogConstants.ButtonsMainAxisSpacing
import com.sf.tadami.ui.components.dialog.alert.AlertDialogContent
import com.sf.tadami.ui.components.dialog.alert.AlertDialogFlowRow
import com.sf.tadami.ui.components.dialog.alert.LocalDismissRequest
import com.sf.tadami.ui.utils.padding

@Composable
fun SimpleDialog(
    modifier: Modifier = Modifier,
    opened : Boolean,
    confirmButton: @Composable (() -> Unit)? = null,
    dismissButton: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    dialogHorizontalPadding : Boolean = true,
    shape: Shape = MaterialTheme.shapes.small,
    containerColor: Color = AlertDialogDefaults.containerColor,
    titleContentColor: Color = AlertDialogDefaults.titleContentColor,
    textContentColor: Color = AlertDialogDefaults.textContentColor,
    tonalElevation: Dp = AlertDialogDefaults.TonalElevation,
    onDismissRequest: () -> Unit,
    text: @Composable BoxScope.() -> Unit = {},
) {
    val isTablet = booleanResource(id = R.bool.is_tablet)
    val maxWidth by remember {
        derivedStateOf {
            if(isTablet) 600.dp else 450.dp
        }
    }

    Crossfade(modifier = Modifier.zIndex(5f),targetState = opened, label = "") { visible ->
        if (visible) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            )
            {
                Surface(modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit){
                        detectTapGestures {
                            onDismissRequest()
                        }
                    }.pointerInput(Unit){
                        detectDragGestures { _, _ -> }
                    },
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.6f)
                ) {}
                AlertDialogContent(
                    modifier = modifier
                        .padding(MaterialTheme.padding.large)
                        .align(Alignment.Center)
                        .sizeIn(minWidth = 300.dp, maxWidth = maxWidth,maxHeight = 400.dp),
                    dialogHorizontalPadding = dialogHorizontalPadding,
                    buttons = if(dismissButton != null || confirmButton != null){
                        {
                            AlertDialogFlowRow(
                                mainAxisSpacing = ButtonsMainAxisSpacing,
                                crossAxisSpacing = ButtonsCrossAxisSpacing
                            ) {
                                CompositionLocalProvider(LocalDismissRequest provides onDismissRequest) {
                                    dismissButton?.let {
                                        it()
                                    }
                                    confirmButton?.invoke()
                                }
                            }
                        }
                    } else null,
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