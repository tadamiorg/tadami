package com.sf.tadami.ui

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import androidx.mediarouter.app.MediaRouteButton
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.activity
import com.google.android.gms.cast.framework.CastButtonFactory
import com.sf.tadami.ui.animeinfos.episode.EpisodeActivity
import com.sf.tadami.ui.animeinfos.episode.cast.castContextOrNull
import com.sf.tadami.ui.animeinfos.episode.cast.setCastIconTint
import com.sf.tadami.ui.webview.WebViewActivity

/** The player and the browser are activities, the cast button the Google Cast one. */
class AndroidPlatformUi(private val app: Application) : PlatformUi {

    override fun NavGraphBuilder.episodeDestination(route: String) {
        activity(route) {
            this.activityClass = EpisodeActivity::class
            argument("sourceId") {
                this.nullable = false
                this.type = NavType.LongType
            }
            argument("episode") {
                this.nullable = false
                this.type = NavType.LongType
            }
        }
    }

    override fun NavGraphBuilder.webViewDestination(route: String) {
        activity(route) {
            this.activityClass = WebViewActivity::class
            argument("sourceId") {
                this.nullable = false
                this.type = NavType.LongType
            }
            argument("title_key") {
                this.nullable = false
                this.type = NavType.StringType
            }
            argument("url_key") {
                this.nullable = false
                this.type = NavType.StringType
            }
        }
    }

    // No cast button without working Google Play services
    override val hasCastButton: Boolean by lazy { castContextOrNull(app) != null }

    @Composable
    override fun CastButton(tint: Color) {
        val argb = tint.toArgb()
        AndroidView(
            factory = { context -> MediaRouteButton(context) },
            update = { mediaButton ->
                CastButtonFactory.setUpMediaRouteButton(app, mediaButton)
                mediaButton.setCastIconTint(argb)
            },
        )
    }
}
