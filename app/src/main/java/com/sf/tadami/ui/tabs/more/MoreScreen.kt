package com.sf.tadami.ui.tabs.more

import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.mediarouter.app.MediaRouteButton
import androidx.navigation.NavHostController
import com.google.android.gms.cast.framework.CastButtonFactory
import com.sf.tadami.R
import com.sf.tadami.navigation.graphs.about.AboutRoutes
import com.sf.tadami.navigation.graphs.more.MORE_ROUTES
import com.sf.tadami.ui.components.widgets.LogoHeader
import com.sf.tadami.ui.components.widgets.ScrollbarLazyColumn
import com.sf.tadami.ui.tabs.more.settings.widget.TextPreference
import com.sf.tadami.ui.utils.padding

@Composable
fun MoreScreen(
    navHostController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Scaffold(
        modifier = modifier,
    ) { contentPadding ->
        ScrollbarLazyColumn(
            modifier = Modifier.padding(contentPadding),
        ) {
            item {
                Box {
                    IconButton(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(
                                end = MaterialTheme.padding.tiny,
                                start = MaterialTheme.padding.tiny,
                                top = MaterialTheme.padding.extraSmall,
                                bottom = 0.dp
                            ),
                        enabled = true,
                        onClick = {},
                    ) {
                        AndroidView(
                            factory = {
                                MediaRouteButton(context)
                            },
                            update = { mediaButton ->
                                CastButtonFactory.setUpMediaRouteButton(
                                    (context as Activity).applicationContext,
                                    mediaButton
                                )
                            }
                        )
                    }
                    LogoHeader(modifier = Modifier.align(Alignment.Center))
                }

            }
            item { HorizontalDivider() }

            item {
                TextPreference(
                    title = stringResource(R.string.label_settings),
                    icon = Icons.Outlined.Settings,
                    onPreferenceClick = {
                        navHostController.navigate(MORE_ROUTES.SETTINGS)
                    },
                )
            }
            item {
                TextPreference(
                    title = stringResource(R.string.label_about),
                    icon = Icons.Outlined.Info,
                    onPreferenceClick = {
                        navHostController.navigate(AboutRoutes.ABOUT)
                    },
                )
            }
        }
    }
}
