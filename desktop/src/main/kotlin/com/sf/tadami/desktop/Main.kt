package com.sf.tadami.desktop

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.sf.tadami.desktop.ui.PrototypeApp
import com.sf.tadami.network.NetworkHelper
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

fun main() {
    DesktopModule.install()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Tadami",
            state = rememberWindowState(width = 1280.dp, height = 820.dp),
        ) {
            setSingletonImageLoaderFactory { context ->
                ImageLoader.Builder(context)
                    .components { add(OkHttpNetworkFetcherFactory(callFactory = { Injekt.get<NetworkHelper>().nonCloudflareClient })) }
                    .build()
            }
            MaterialTheme(colorScheme = darkColorScheme()) {
                PrototypeApp()
            }
        }
    }
}
