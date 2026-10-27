package com.sf.tadami.ui.animeinfos.episode.cast.channels

import com.sf.tadami.preferences.player.PlayerPreferences
import kotlinx.serialization.Serializable

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

/** Receiver → phone handshake reply. Only the web receiver sends this. */
@Serializable
data class ReceiverHandshakeMessage(
    /** "web" (the native receiver never replies). */
    val receiverType: String = "",
    val receiverProtocol: Int = 0,
    val receiverVersion: String = "",
    /** Optional features of the receiver (e.g. "seekPreview"); absent from older receivers. */
    val capabilities: List<String> = emptyList(),
)
