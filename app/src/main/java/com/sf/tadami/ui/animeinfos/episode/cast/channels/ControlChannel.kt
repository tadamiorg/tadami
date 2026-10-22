package com.sf.tadami.ui.animeinfos.episode.cast.channels

import android.util.Log
import com.google.android.gms.cast.CastDevice
import com.sf.tadami.preferences.player.PlayerPreferences
import com.sf.tadami.ui.animeinfos.episode.cast.preview.CastPreviewProtocol
import com.sf.tadami.ui.animeinfos.episode.cast.preview.CastPreviewRequest
import com.sf.tadami.ui.animeinfos.episode.cast.preview.CastPreviewSource
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Receives control messages from the `tadami-tv-cast` receiver: playback progress (so the phone
 * can persist watch time to its DB) and episode-navigation requests (next / previous / pick),
 * which the phone fulfils by re-resolving sources and re-loading them onto the TV.
 */
class ControlChannel(
    /** Seekbar preview requests of the web receiver ([CastPreviewProtocol]), apart from the other messages. */
    private val onPreviewRequest: (CastPreviewRequest) -> Unit = {},
    /** The stream the web receiver plays, for the seekbar preview prefetch. */
    private val onPreviewSource: (CastPreviewSource) -> Unit = {},
    private val onControl: (TvControlMessage) -> Unit,
) : CustomCastChannel() {

    override val namespace: String
        get() = NAMESPACE

    override fun onMessageReceived(castDevice: CastDevice, namespace: String, message: String) {
        runCatching { json.decodeFromString<TvControlMessage>(message) }
            .onSuccess { control ->
                when (control.type) {
                    CastPreviewProtocol.TYPE_REQUEST -> CastPreviewProtocol.decodeRequest(message)?.let(onPreviewRequest)
                        ?: Log.d("ControlChannel", "Unusable preview request")
                    CastPreviewProtocol.TYPE_SOURCE -> CastPreviewProtocol.decodeSource(message)?.let(onPreviewSource)
                        ?: Log.d("ControlChannel", "Unusable preview source")
                    else -> onControl(control)
                }
            }
            .onFailure { Log.d("ControlChannel", "Failed to parse control message", it) }
    }

    companion object {
        const val NAMESPACE: String = "urn:x-cast:com.sf.tadami.control"
        private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    }
}

@Serializable
data class TvControlMessage(
    /**
     * TV→phone: "progress" | "save" | "next" | "previous" | "selectEpisode" | "state" (and
     * "previewSource", a CastPreviewSource, and "previewRequest", a CastPreviewRequest).
     * phone→TV: "selectSource" | "selectSubtitle" | "selectAudio" | "subtitleStyle" (and
     * "previewFrame", a CastPreviewFrame).
     */
    val type: String,
    val position: Long = 0L,
    val duration: Long = 0L,
    val playing: Boolean = false,
    val episodeId: Long? = null,
    /** Index into the current source's list. subtitleIndex == -1 means subtitles off. */
    val sourceIndex: Int? = null,
    val subtitleIndex: Int? = null,
    val audioIndex: Int? = null,
    val subtitleStyle: CastSubtitleStyle? = null,
)

/**
 * Subtitle style forwarded phone→TV so the receiver's overlay mirrors the phone's preferences.
 *
 * Values are the raw preferences, with the SAME meaning the phone's own renderer gives them
 * (PlayerSubtitleView): [textSize] in sp, [outlineWidth] and [letterSpacing] in hundredths
 * (percent of text size / em). Defaults mirror PlayerPreferences so an unstyled load still matches.
 */
@Serializable
data class CastSubtitleStyle(
    val textSize: Int = PlayerPreferences.DEFAULT_TEXT_SIZE,
    val fontWeight: Int = PlayerPreferences.DEFAULT_FONT_WEIGHT,
    val outlineWidth: Int = PlayerPreferences.DEFAULT_OUTLINE_WIDTH,
    val letterSpacing: Int = PlayerPreferences.DEFAULT_LETTER_SPACING,
    val textColor: Int = 0,
    val edgeColor: Int = 0,
    val italic: Boolean = false,
)

/** The subtitle look the phone is rendering locally, as the cast payload. */
fun PlayerPreferences.toCastSubtitleStyle(): CastSubtitleStyle = CastSubtitleStyle(
    textSize = subtitleTextSize,
    fontWeight = subtitleFontWeight,
    outlineWidth = subtitleOutlineWidth,
    letterSpacing = subtitleLetterSpacing,
    textColor = subtitleTextColor,
    edgeColor = subtitleEdgeColor,
    italic = subtitleItalicFormat,
)
