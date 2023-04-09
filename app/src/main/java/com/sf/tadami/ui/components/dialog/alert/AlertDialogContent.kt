package com.sf.tadami.ui.components.dialog.alert

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.booleanResource
import com.sf.tadami.R
import com.sf.tadami.ui.components.dialog.alert.AlertDialogConstants.DialogFooterPadding
import com.sf.tadami.ui.components.dialog.alert.AlertDialogConstants.DialogTextPadding
import com.sf.tadami.ui.components.dialog.alert.AlertDialogConstants.DialogTitlePadding
import com.sf.tadami.ui.utils.plus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AlertDialogContent(
    buttons: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    title: (@Composable () -> Unit)?,
    text: @Composable (() -> Unit)?,
    containerColor: Color,
    buttonContentColor: Color,
    titleContentColor: Color,
    textContentColor: Color,
) {
    val configuration = LocalConfiguration.current

    val isTablet = booleanResource(id = R.bool.is_tablet)
    
    val heightFraction = remember {
        when(configuration.orientation){
            Configuration.ORIENTATION_LANDSCAPE -> if(isTablet) 0.65f else 0.9f
            else -> if(isTablet) 0.5f else 0.7f
        }
    }

    BoxWithConstraints(modifier = Modifier
        .fillMaxHeight(heightFraction)
        .wrapContentHeight(), contentAlignment = Alignment.Center) {
        Scaffold(
            modifier = modifier
                .clip(MaterialTheme.shapes.small),
            containerColor = containerColor,
            topBar = {
                title?.let {
                    CompositionLocalProvider(LocalContentColor provides titleContentColor) {
                        val textStyle = MaterialTheme.typography.headlineSmall
                        ProvideTextStyle(textStyle) {
                            Box(
                                Modifier.padding(DialogTitlePadding)
                            ) {
                                title()
                            }
                        }
                    }
                }
            },
            bottomBar = {
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .padding(DialogFooterPadding), horizontalArrangement = Arrangement.End) {
                    CompositionLocalProvider(LocalContentColor provides buttonContentColor) {
                        val textStyle =
                            MaterialTheme.typography.labelLarge
                        ProvideTextStyle(value = textStyle, content = buttons)
                    }
                }
            }
        ) { padding ->
            text?.let {
                CompositionLocalProvider(LocalContentColor provides textContentColor) {
                    val textStyle =
                        MaterialTheme.typography.bodyMedium
                    ProvideTextStyle(textStyle) {
                        Box(
                            modifier = Modifier
                                .padding(padding + DialogTextPadding)
                        ) {
                            text()
                        }
                    }
                }
            }
        }
    }

}