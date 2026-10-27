package com.sf.tadami.ui.animeinfos.episode.player.subtitles

import android.content.Context
import android.graphics.Typeface
import androidx.annotation.OptIn
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.res.ResourcesCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.CaptionStyleCompat
import com.sf.tadami.R
import com.sf.tadami.preferences.player.PlayerPreferences

@OptIn(UnstableApi::class)
fun PlayerPreferences.toSubtitlesStyle(context: Context): CaptionStyleCompat {
    return CaptionStyleCompat(
        this.subtitleTextColor,
        this.subtitleBackgroundColor,
        Color.Transparent.toArgb(), // Edge color handled separately
        this.subtitleEdgeType,
        this.subtitleEdgeColor,
        getCustomTypeface(context) // We'll set the typeface through PlayerView
    )
}

private fun getCustomTypeface(context: Context): Typeface {
    return ResourcesCompat.getFont(context, R.font.roboto_extrabold) ?: Typeface.DEFAULT
}
