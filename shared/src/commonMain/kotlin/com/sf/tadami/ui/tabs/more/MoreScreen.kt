package com.sf.tadami.ui.tabs.more

import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.Injekt
import com.sf.tadami.ui.PlatformUi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.sf.tadami.shared.resources.Res
import com.sf.tadami.shared.resources.*
import com.sf.tadami.navigation.graphs.about.AboutRoutes
import com.sf.tadami.navigation.graphs.more.MORE_ROUTES
import com.sf.tadami.preferences.app.BasePreferences
import com.sf.tadami.preferences.model.rememberDataStoreState
import com.sf.tadami.ui.components.widgets.LogoHeader
import com.sf.tadami.ui.components.widgets.ScrollbarLazyColumn
import com.sf.tadami.ui.tabs.more.settings.widget.TextPreference
import com.sf.tadami.ui.tabs.more.settings.widget.TogglePreference
import com.sf.tadami.ui.utils.padding

@Composable
fun MoreScreen(
    navHostController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val platformUi = remember { Injekt.get<PlatformUi>() }
    val basePreferencesState = rememberDataStoreState(BasePreferences)
    val basePreferences by basePreferencesState.value.collectAsState()
    Scaffold(
        modifier = modifier,
    ) { contentPadding ->
        ScrollbarLazyColumn(
            modifier = Modifier.padding(contentPadding),
        ) {
            item {
                Box {
                    // No cast button without working Google Play services
                    if (platformUi.hasCastButton) IconButton(
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
                        platformUi.CastButton(LocalContentColor.current)
                    }
                    LogoHeader(modifier = Modifier.align(Alignment.Center))
                }

            }
            item { HorizontalDivider() }

            item {
                TogglePreference(
                    title = stringResource(Res.string.label_downloaded_only),
                    subtitle = stringResource(Res.string.downloaded_only_summary),
                    icon = Icons.Outlined.CloudOff,
                    checked = basePreferences.downloadedOnly,
                    onCheckedChanged = {
                        basePreferencesState.setValue(basePreferences.copy(downloadedOnly = it))
                    },
                )
            }
            item { HorizontalDivider() }

            item {
                TextPreference(
                    title = stringResource(Res.string.label_download_queue),
                    icon = Icons.Outlined.Download,
                    onPreferenceClick = {
                        navHostController.navigate(MORE_ROUTES.DOWNLOAD_QUEUE)
                    },
                )
            }
            item {
                TextPreference(
                    title = stringResource(Res.string.label_settings),
                    icon = Icons.Outlined.Settings,
                    onPreferenceClick = {
                        navHostController.navigate(MORE_ROUTES.SETTINGS)
                    },
                )
            }
            item {
                TextPreference(
                    title = stringResource(Res.string.label_about),
                    icon = Icons.Outlined.Info,
                    onPreferenceClick = {
                        navHostController.navigate(AboutRoutes.ABOUT)
                    },
                )
            }
        }
    }
}
