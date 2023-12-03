package com.sf.tadami.ui.components.dialog.alert

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import com.sf.tadami.R
import com.sf.tadami.ui.components.dialog.alert.AlertDialogConstants.DialogFooterPadding
import com.sf.tadami.ui.components.dialog.alert.AlertDialogConstants.DialogHorizontalPadding
import com.sf.tadami.ui.components.dialog.alert.AlertDialogConstants.DialogTitlePadding

@Composable
internal fun AlertDialogContent(
    buttons: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    text: @Composable ColumnScope.() -> Unit,
    shape: Shape,
    containerColor: Color,
    tonalElevation: Dp,
    buttonContentColor: Color,
    titleContentColor: Color,
    textContentColor: Color,
) {
    val configuration = LocalConfiguration.current


    val isTablet = booleanResource(id = R.bool.is_tablet)

    val heightFraction = remember {
        when (configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> if (isTablet) 0.7f else 0.9f
            else -> if (isTablet) 0.65f else 0.7f
        }
    }
    val maxHeight = remember(configuration.screenHeightDp,heightFraction) {
        derivedStateOf {
            configuration.screenHeightDp.dp * heightFraction
        }
    }


    Surface(
        modifier = modifier,
        shape = shape,
        color = containerColor,
        tonalElevation = tonalElevation,
    ) {
        Column(
            modifier = Modifier
                .requiredHeightIn(max = maxHeight.value)
                .padding(DialogHorizontalPadding)
        ) {
            Box(
                // Align the title to the center when an icon is present.
                Modifier
                    .padding(DialogTitlePadding)
                    .align(Alignment.Start)
                    .weight(1f, false)
            ) {
                CompositionLocalProvider(LocalContentColor provides titleContentColor) {
                    val textStyle = MaterialTheme.typography.headlineSmall
                    ProvideTextStyle(textStyle) {
                        title()
                    }
                }
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f, false)
            ) {
                CompositionLocalProvider(LocalContentColor provides textContentColor) {
                    val textStyle =
                        MaterialTheme.typography.bodyMedium
                    ProvideTextStyle(textStyle) {
                        text()
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(DialogFooterPadding)
            ) {
                CompositionLocalProvider(LocalContentColor provides buttonContentColor) {
                    val textStyle =
                        MaterialTheme.typography.labelLarge
                    ProvideTextStyle(value = textStyle, content = buttons)
                }
            }
        }
    }
}
