package com.sf.tadami.ui.animeinfos.episode.player.subtitles

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import androidx.media3.common.text.Cue
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi

/** Style for the custom subtitle renderer. Colors are Compose [Color]; sizes in sp/dp. */
data class SubtitleStyle(
    val textSize: Float,
    val textColor: Color,
    val outlineColor: Color,
    /** Compose font weight (100..900). */
    val fontWeight: Int = 700,
    val italic: Boolean = false,
    /** Extra letter spacing in em. */
    val letterSpacing: Float = 0f,
    /** Bottom margin as a fraction of the player height (keeps subtitles above the very bottom edge). */
    val bottomPaddingFraction: Float = 0.06f,
    /** Outline stroke width as a fraction of the text size (independent of media3's fixed ~0.1). */
    val outlineFraction: Float = 0.18f,
)

/** Line-height multiplier applied to the text; also used to space stacked cues by their [Cue.line] row. */
private const val LINE_HEIGHT_FACTOR = 1.3f

/**
 * Renders the player's current subtitle cues with a full, thick outline around every letter — a Compose
 * replacement for ExoPlayer's built-in SubtitleView. It only *renders*; the player still selects/parses the text
 * track and emits its cues via [Player.Listener.onCues].
 *
 * Each cue is placed at an absolute vertical offset derived from its [Cue.line] (set by the WebVTT parser:
 * LINE_TYPE_NUMBER, `-2` = bottom row, more negative = higher). This avoids a reflowing column, so a cue never
 * shifts when another cue appears or ends.
 */
@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(UnstableApi::class)
@Composable
fun PlayerSubtitleView(
    player: Player,
    style: SubtitleStyle,
    modifier: Modifier = Modifier,
) {
    var cues by remember { mutableStateOf(player.currentCues.cues) }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onCues(cueGroup: CueGroup) {
                cues = cueGroup.cues
            }
        }
        player.addListener(listener)
        cues = player.currentCues.cues
        onDispose { player.removeListener(listener) }
    }

    val visibleCues = cues.filter { !it.text.isNullOrBlank() }
    if (visibleCues.isEmpty()) return

    val lineHeightDp = with(LocalDensity.current) { (style.textSize * LINE_HEIGHT_FACTOR).sp.toDp() }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val baseBottom = maxHeight * style.bottomPaddingFraction
        visibleCues.forEach { cue ->
            // Parser's line is the cue's bottom row: -2 -> row 0 (bottom), -3 -> one row up, etc.
            val rows = if (cue.line != Cue.DIMEN_UNSET && cue.lineType == Cue.LINE_TYPE_NUMBER) {
                (-cue.line - 2f).coerceAtLeast(0f)
            } else {
                0f
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(0.92f)
                    .padding(bottom = baseBottom + lineHeightDp * rows),
                contentAlignment = Alignment.BottomCenter,
            ) {
                OutlinedSubtitleLine(cue.text.toString(), style)
            }
        }
    }
}

@Composable
private fun OutlinedSubtitleLine(text: String, style: SubtitleStyle) {
    val outlinePx = with(LocalDensity.current) { style.textSize.sp.toPx() * style.outlineFraction }
    val base = TextStyle(
        fontSize = style.textSize.sp,
        lineHeight = (style.textSize * LINE_HEIGHT_FACTOR).sp,
        fontWeight = FontWeight(style.fontWeight.coerceIn(1, 1000)),
        fontStyle = if (style.italic) FontStyle.Italic else FontStyle.Normal,
        letterSpacing = style.letterSpacing.em,
        textAlign = TextAlign.Center,
    )
    Box(contentAlignment = Alignment.Center) {
        // Outline (stroked) layer behind.
        Text(
            text = text,
            style = base.copy(
                color = style.outlineColor,
                drawStyle = Stroke(width = outlinePx, join = StrokeJoin.Round, cap = StrokeCap.Round),
            ),
        )
        // Fill layer on top.
        Text(text = text, style = base.copy(color = style.textColor))
    }
}
