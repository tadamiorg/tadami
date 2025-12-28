package com.sf.tadami.ui.tabs.more

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import com.sf.tadami.R
import com.sf.tadami.navigation.graphs.about.AboutRoutes
import com.sf.tadami.navigation.graphs.more.MORE_ROUTES
import com.sf.tadami.ui.components.widgets.LogoHeader
import com.sf.tadami.ui.components.widgets.ScrollbarLazyColumn
import com.sf.tadami.ui.tabs.more.settings.widget.TextPreference

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
