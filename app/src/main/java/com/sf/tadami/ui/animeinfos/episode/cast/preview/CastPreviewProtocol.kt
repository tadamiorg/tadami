package com.sf.tadami.ui.animeinfos.episode.cast.preview

import com.sf.tadami.ui.animeinfos.episode.player.preview.PREVIEW_FRAME_WIDTH_PX
import com.sf.tadami.ui.animeinfos.episode.player.preview.previewBucketRangeMs
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Base64
import kotlin.math.roundToInt

/**
 * Seekbar preview frames for the web cast receiver, over the control channel
 * (`urn:x-cast:com.sf.tadami.control`). Negotiated, so old phones and old receivers never see it:
 * - the web receiver announces [CAPABILITY] in its handshake reply (`capabilities`);
 * - the phone announces it in the LOAD customData (`seekPreview: true`) while its "Seekbar preview"
 *   setting is on.
 * The receiver then asks for one preview step at a time ([CastPreviewRequest], steps as in
 * previewBucketSizeMs, from the duration it sends) and the phone answers each request once
 * ([CastPreviewFrame]): the step's frame as a base64 JPEG, about [PREVIEW_FRAME_WIDTH_PX] wide, or a
 * status without one.
 */
object CastPreviewProtocol {
    const val CAPABILITY = "seekPreview"
    const val TYPE_REQUEST = "previewRequest"
    const val TYPE_FRAME = "previewFrame"

    /** The frame is in the reply. */
    const val STATUS_OK = "ok"

    /** This step has no frame (none in time): the receiver doesn't ask for it again. */
    const val STATUS_NONE = "none"

    /** No preview for this stream (setting off, Wi-Fi only, the stream can't be decoded). */
    const val STATUS_OFF = "off"

    /**
     * Base64 characters of a frame at most. A cast message is limited to 64 KB: the frame keeps well
     * under it, room left for the rest of the reply (the stream's id holds its URL).
     */
    const val MAX_FRAME_BASE64_CHARS = 40_000

    /** Characters of a whole reply at most, a margin under the 64 KB of a cast message. */
    const val MAX_MESSAGE_CHARS = 60_000

    /** JPEG qualities tried in turn, until the frame fits [MAX_FRAME_BASE64_CHARS]. */
    val JPEG_QUALITIES = listOf(80, 65, 50, 35)

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
    }

    /** The request in a control message, null when it isn't a usable one. */
    fun decodeRequest(message: String): CastPreviewRequest? =
        runCatching { json.decodeFromString<CastPreviewRequest>(message) }.getOrNull()?.takeIf { it.isValid }

    /** The reply carrying [frame]; null when it wouldn't fit a cast message. */
    fun frameMessage(request: CastPreviewRequest, frame: PreviewJpeg): String? {
        val message = json.encodeToString(
            CastPreviewFrame(
                requestId = request.requestId,
                sourceId = request.sourceId,
                step = request.step,
                status = STATUS_OK,
                width = frame.width,
                height = frame.height,
                jpeg = Base64.getEncoder().encodeToString(frame.bytes),
            )
        )
        return message.takeIf { it.length <= MAX_MESSAGE_CHARS }
    }

    /** A reply without a frame: [status] is [STATUS_NONE] or [STATUS_OFF]. */
    fun statusMessage(request: CastPreviewRequest, status: String): String = json.encodeToString(
        CastPreviewFrame(requestId = request.requestId, sourceId = request.sourceId, step = request.step, status = status)
    )

    /** Length of [bytes] bytes in base64. */
    fun base64Length(bytes: Int): Int = (bytes + 2) / 3 * 4

    /**
     * The JPEG of a frame within [MAX_FRAME_BASE64_CHARS]: [encode] at each of [qualities] in turn
     * until one fits, null when none does.
     */
    fun encodeWithinBudget(
        qualities: List<Int> = JPEG_QUALITIES,
        maxBase64Chars: Int = MAX_FRAME_BASE64_CHARS,
        encode: (quality: Int) -> ByteArray?,
    ): ByteArray? = qualities.asSequence()
        .mapNotNull(encode)
        .firstOrNull { base64Length(it.size) <= maxBase64Chars }
}

/** receiver → phone: the frame of preview step [step] of the stream [url] ([headers] its source's). */
@Serializable
data class CastPreviewRequest(
    val type: String = "",
    val requestId: Long = -1,
    /** Opaque id of the stream the TV plays (episode, url, server, quality): the frames' scope. */
    val sourceId: String = "",
    /** The source's URL as the phone sent it: online, or a proxy `/local-` URL of a downloaded episode. */
    val url: String = "",
    /** The source's headers as the phone sent them; null when it has none. */
    val headers: Map<String, String>? = null,
    val step: Int = -1,
    /** Duration of the stream on the receiver, from which the steps are made. */
    val duration: Long = 0,
) {
    val isValid: Boolean
        get() = type == CastPreviewProtocol.TYPE_REQUEST && sourceId.isNotBlank() && url.isNotBlank() &&
            duration > 0 && step >= 0 && previewBucketRangeMs(step, duration).first < duration
}

/** phone → receiver: the answer to a [CastPreviewRequest], echoing its id, stream and step. */
@Serializable
data class CastPreviewFrame(
    val type: String = CastPreviewProtocol.TYPE_FRAME,
    val requestId: Long,
    val sourceId: String,
    val step: Int,
    val status: String,
    val width: Int? = null,
    val height: Int? = null,
    /** Base64 JPEG, with [CastPreviewProtocol.STATUS_OK] only. */
    val jpeg: String? = null,
)

/** An encoded preview frame. */
class PreviewJpeg(val bytes: ByteArray, val width: Int, val height: Int)

/**
 * Size of a preview frame for a video of [aspectRatio] (width / height): [PREVIEW_FRAME_WIDTH_PX]
 * wide, or as high for a portrait video.
 */
fun previewFrameSize(aspectRatio: Float): Pair<Int, Int> {
    val ratio = aspectRatio.takeIf { it.isFinite() && it > 0f } ?: (16f / 9f)
    val max = PREVIEW_FRAME_WIDTH_PX
    return if (ratio >= 1f) {
        max to (max / ratio).roundToInt().coerceIn(1, max)
    } else {
        (max * ratio).roundToInt().coerceIn(1, max) to max
    }
}

/**
 * Whether the phone makes the frames a request asks for: the "Seekbar preview" setting is on, the
 * receiver announced [CastPreviewProtocol.CAPABILITY], and, for an online stream, "Only on Wi-Fi" is
 * off or the phone is on Wi-Fi (a downloaded episode needs no network, as in the phone player).
 */
fun castSeekPreviewAllowed(
    settingOn: Boolean,
    onlyOnWifi: Boolean,
    onWifi: Boolean,
    receiverCapable: Boolean,
    localFile: Boolean,
): Boolean = settingOn && receiverCapable && (localFile || !onlyOnWifi || onWifi)
