package com.sf.tadami.preferences.player

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.CaptionStyleCompat
import com.sf.tadami.R
import com.sf.tadami.preferences.model.CustomPreferences
import com.sf.tadami.preferences.model.CustomPreferencesIdentifier

@UnstableApi
data class PlayerPreferences(
    val seenThreshold: Int,
    val doubleTapLength: Long,
    val autoPlay: Boolean,
    val ignoreCutout: Boolean,
    val subtitlesEnabled: Boolean,
    val subtitlePrefLanguages: String,
    val subtitleTextSize: Int,
    val subtitleTextColor: Int,
    val subtitleBackgroundColor: Int,
    val subtitleEdgeType: Int,
    val subtitleEdgeColor: Int,
    val subtitleBoldFormat: Boolean,
    val subtitleItalicFormat: Boolean,
    val subtitleBottomPadding: Float,
) : CustomPreferencesIdentifier {

    object SeenThresholdItems {
        const val SEVENTY = 70
        const val SEVENTY_FIVE = 75
        const val EIGHTY = 80
        const val EIGHTY_FIVE = 85
        const val NINETY = 90
        const val NINETY_FIVE = 95
        const val HUNDRED = 100
    }
    object DoubleTapLengthItems {
        const val FIVE = 5000L
        const val TEN = 10000L
        const val FIFTEEN = 15000L
        const val TWENTY = 20000L
        const val TWENTY_FIVE = 25000L
        const val THIRTY = 30000L
    }



    companion object : CustomPreferences<PlayerPreferences> {
        val DefaultSubtitlesPrefLanguages = mapOf(
            "fr" to R.string.language_fr,
            "en" to R.string.language_en
        )

        // Subtitles default style
        private const val DEFAULT_TEXT_SIZE = 22
        private val DEFAULT_TEXT_COLOR = Color.White.toArgb()
        private val DEFAULT_BACKGROUND_COLOR = Color.Transparent.toArgb()
        private const val DEFAULT_EDGE_TYPE = CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW
        private val DEFAULT_EDGE_COLOR = Color.Black.toArgb()
        private  val DEFAULT_BOTTOM_PADDING = 10f


        private val SEEN_THRESHOLD =  intPreferencesKey("player_seen_threshold")
        private val DOUBLE_TAP_LENGTH = longPreferencesKey("player_double_tap_length")
        private val AUTO_PLAY = booleanPreferencesKey("player_auto_play")
        private val IGNORE_CUTOUT = booleanPreferencesKey("player_ignore_cutout")
        private val SUBTITLES_ENABLED = booleanPreferencesKey("player_subtitles_enabled")
        private val SUBTITLES_PREF_LANGUAGES = stringPreferencesKey("player_subtitles_pref_languages")
        private val SUBTITLES_TEXT_SIZE = intPreferencesKey("player_subtitles_text_size")
        private val SUBTITLES_TEXT_COLOR = intPreferencesKey("player_subtitles_text_color")
        private val SUBTITLES_BACKGROUND_COLOR = intPreferencesKey("player_subtitles_background_color")
        private val SUBTITLES_EDGE_TYPE = intPreferencesKey("player_subtitles_edge_type")
        private val SUBTITLES_EDGE_COLOR = intPreferencesKey("player_subtitles_edge_color")
        private val SUBTITLE_BOLD_FORMAT = booleanPreferencesKey("player_subtitles_bold_format")
        private val SUBTITLE_ITALIC_FORMAT = booleanPreferencesKey("player_subtitles_italic_format")
        private val SUBTITLE_BOTTOM_PADDING = floatPreferencesKey("player_subtitles_bottom_padding")

        override fun transform(preferences: Preferences): PlayerPreferences {
           return PlayerPreferences(
               seenThreshold = preferences[SEEN_THRESHOLD] ?: SeenThresholdItems.EIGHTY_FIVE,
               doubleTapLength = preferences[DOUBLE_TAP_LENGTH] ?: DoubleTapLengthItems.TEN,
               autoPlay = preferences[AUTO_PLAY] ?: false,
               subtitlesEnabled = preferences[SUBTITLES_ENABLED] ?: true,
               subtitlePrefLanguages = preferences[SUBTITLES_PREF_LANGUAGES] ?: DefaultSubtitlesPrefLanguages.keys.joinToString(separator = ","),
               subtitleTextSize = preferences[SUBTITLES_TEXT_SIZE] ?: DEFAULT_TEXT_SIZE,
               subtitleTextColor = preferences[SUBTITLES_TEXT_COLOR] ?: DEFAULT_TEXT_COLOR,
               subtitleBackgroundColor = preferences[SUBTITLES_BACKGROUND_COLOR] ?: DEFAULT_BACKGROUND_COLOR,
               subtitleEdgeType = preferences[SUBTITLES_EDGE_TYPE] ?: DEFAULT_EDGE_TYPE,
               subtitleEdgeColor = preferences[SUBTITLES_EDGE_COLOR] ?: DEFAULT_EDGE_COLOR,
               subtitleBoldFormat = preferences[SUBTITLE_BOLD_FORMAT] ?: false,
               subtitleItalicFormat = preferences[SUBTITLE_ITALIC_FORMAT] ?: false,
               subtitleBottomPadding = preferences[SUBTITLE_BOTTOM_PADDING] ?: DEFAULT_BOTTOM_PADDING,
               ignoreCutout = preferences[IGNORE_CUTOUT] ?: true
           )
        }

        override fun setPrefs(newValue: PlayerPreferences, preferences: MutablePreferences) {
            preferences[SEEN_THRESHOLD] = newValue.seenThreshold
            preferences[DOUBLE_TAP_LENGTH] = newValue.doubleTapLength
            preferences[AUTO_PLAY] = newValue.autoPlay
            preferences[SUBTITLES_ENABLED] = newValue.subtitlesEnabled
            preferences[SUBTITLES_PREF_LANGUAGES] = newValue.subtitlePrefLanguages
            preferences[SUBTITLES_TEXT_SIZE] = newValue.subtitleTextSize
            preferences[SUBTITLES_TEXT_COLOR] = newValue.subtitleTextColor
            preferences[SUBTITLES_BACKGROUND_COLOR] = newValue.subtitleBackgroundColor
            preferences[SUBTITLES_EDGE_TYPE] = newValue.subtitleEdgeType
            preferences[SUBTITLES_EDGE_COLOR] = newValue.subtitleEdgeColor
            preferences[SUBTITLE_BOLD_FORMAT] = newValue.subtitleBoldFormat
            preferences[SUBTITLE_ITALIC_FORMAT] = newValue.subtitleItalicFormat
            preferences[SUBTITLE_BOTTOM_PADDING] = newValue.subtitleBottomPadding
            preferences[IGNORE_CUTOUT] = newValue.ignoreCutout
        }
    }
}