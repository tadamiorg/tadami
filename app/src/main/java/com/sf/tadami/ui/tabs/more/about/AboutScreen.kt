package com.sf.tadami.ui.tabs.more.about

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.sf.tadami.BuildConfig
import com.sf.tadami.R
import com.sf.tadami.data.update.AppUpdater.Companion.RELEASE_URL
import com.sf.tadami.ui.components.customIcons.GithubIcon
import com.sf.tadami.ui.components.topappbar.TadaTopAppBar
import com.sf.tadami.ui.components.widgets.LinkIcon
import com.sf.tadami.ui.components.widgets.LogoHeader
import com.sf.tadami.ui.components.widgets.ScrollbarLazyColumn
import com.sf.tadami.ui.tabs.more.settings.widget.TextPreference
import com.sf.tadami.ui.utils.toDateTimestampString
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    navHostController: NavHostController,
    aboutViewModel: AboutViewModel = viewModel()
) {
    val uriHandler = LocalUriHandler.current
    var isCheckingUpdates by remember { mutableStateOf(false) }
    val uiState by aboutViewModel.appUpdaterUiState.collectAsState()
    if (uiState.shouldShowUpdateDialog && uiState.updateInfos?.info != null) {
        AppUpdateDialog(
            onDismissRequest = {
                aboutViewModel.hideDialog()
            },
            uiState = uiState
        )
    }

    Scaffold(
        topBar = {
            TadaTopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.label_about),
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navHostController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = null,
                        )
                    }
                }
            )
        },
    ) { contentPadding ->
        ScrollbarLazyColumn(
            contentPadding = contentPadding,
        ) {
            item {
                LogoHeader()
            }

            item {
                TextPreference(
                    title = stringResource(R.string.version),
                    subtitle = getVersionName(withBuildDate = true)
                )
            }


            item {
                TextPreference(
                    title = stringResource(R.string.check_for_updates),
                    widget = {
                        AnimatedVisibility(visible = isCheckingUpdates) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 3.dp,
                            )
                        }
                    },
                    onPreferenceClick = {
                        if (!isCheckingUpdates) {
                            isCheckingUpdates = true
                            aboutViewModel.checkVersion(
                                onFinish = {
                                    isCheckingUpdates = false
                                },
                            )
                        }
                    },
                )
            }



            item {
                TextPreference(
                    title = stringResource(R.string.whats_new),
                    onPreferenceClick = { uriHandler.openUri(RELEASE_URL) },
                )
            }


            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    LinkIcon(
                        label = "GitHub",
                        icon = GithubIcon,
                        url = "https://github.com/tadamiorg/tadami",
                    )
                }
            }
        }
    }
}

/**
 * Checks version and shows a user prompt if an update is available.
 */


fun getVersionName(withBuildDate: Boolean): String {
    return "Stable ${BuildConfig.VERSION_NAME}".let {
        if (withBuildDate) {
            "$it (${getFormattedBuildTime()})"
        } else {
            it
        }
    }

}

internal fun getFormattedBuildTime(): String {
    return try {
        val inputDf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        inputDf.timeZone = TimeZone.getTimeZone("UTC")
        val buildTime = inputDf.parse(BuildConfig.BUILD_DATE)

        val outputDf = DateFormat.getDateTimeInstance(
            DateFormat.MEDIUM,
            DateFormat.SHORT,
            Locale.getDefault(),
        )
        outputDf.timeZone = TimeZone.getDefault()

        buildTime!!.toDateTimestampString(
            DateFormat.getDateInstance(DateFormat.SHORT)
        )
    } catch (e: Exception) {
        BuildConfig.BUILD_DATE
    }
}
