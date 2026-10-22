package com.sf.tadami.ui.animeinfos.episode.cast.preview

import com.sf.tadami.ui.animeinfos.episode.cast.channels.TvControlMessage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

/** The wire contract of the cast seekbar preview, as the web receiver sends and reads it. */
class CastPreviewProtocolTest {

    private val controlJson = Json { ignoreUnknownKeys = true; isLenient = true }

    // What the receiver's PreviewController sends (tadami-cast-receiver src/preview/previewController.ts)
    private val receiverRequest = """
        {"type":"previewRequest","requestId":3,"sourceId":"7|https://h.example/ep.m3u8|Server A|1080p",
         "url":"https://h.example/ep.m3u8","headers":{"Referer":"https://h.example/"},"step":12,"duration":1420000}
    """.trimIndent()

    @Test
    fun `decodes the receiver's request`() {
        val request = CastPreviewProtocol.decodeRequest(receiverRequest)
        assertNotNull(request)
        request!!
        assertEquals(3L, request.requestId)
        assertEquals("7|https://h.example/ep.m3u8|Server A|1080p", request.sourceId)
        assertEquals("https://h.example/ep.m3u8", request.url)
        assertEquals(mapOf("Referer" to "https://h.example/"), request.headers)
        assertEquals(12, request.step)
        assertEquals(1_420_000L, request.duration)
    }

    @Test
    fun `the control channel tells a request from the other messages by its type`() {
        // ControlChannel decodes every message as a TvControlMessage first: a request must survive it
        val control = controlJson.decodeFromString<TvControlMessage>(receiverRequest)
        assertEquals(CastPreviewProtocol.TYPE_REQUEST, control.type)
        assertNull(CastPreviewProtocol.decodeRequest("""{"type":"progress","position":1,"duration":2}"""))
    }

    @Test
    fun `a source without headers sends null`() {
        val request = CastPreviewProtocol.decodeRequest(receiverRequest.replace("""{"Referer":"https://h.example/"}""", "null"))
        assertNull(request!!.headers)
    }

    @Test
    fun `refuses unusable requests`() {
        listOf(
            receiverRequest.replace("\"step\":12", "\"step\":-1"),
            // 1% of 1420 s is 14.2 s: step 100 starts past the end
            receiverRequest.replace("\"step\":12", "\"step\":100"),
            receiverRequest.replace("\"duration\":1420000", "\"duration\":0"),
            receiverRequest.replace("\"url\":\"https://h.example/ep.m3u8\"", "\"url\":\"\""),
            receiverRequest.replace("\"sourceId\":\"7|https://h.example/ep.m3u8|Server A|1080p\"", "\"sourceId\":\"\""),
            """{"requestId":3,"sourceId":"s","url":"u","step":1,"duration":60000}""",
            "not json",
        ).forEach { assertNull(it, CastPreviewProtocol.decodeRequest(it)) }
    }

    @Test
    fun `a frame reply carries the request's id, stream and step with the JPEG in base64`() {
        val request = CastPreviewProtocol.decodeRequest(receiverRequest)!!
        val jpeg = ByteArray(1_000) { it.toByte() }
        val message = CastPreviewProtocol.frameMessage(request, PreviewJpeg(jpeg, 256, 144))!!
        val reply = Json.parseToJsonElement(message).jsonObject
        assertEquals("previewFrame", reply["type"]!!.jsonPrimitive.content)
        assertEquals(3L, reply["requestId"]!!.jsonPrimitive.long)
        assertEquals(request.sourceId, reply["sourceId"]!!.jsonPrimitive.content)
        assertEquals(12, reply["step"]!!.jsonPrimitive.int)
        assertEquals("ok", reply["status"]!!.jsonPrimitive.content)
        assertEquals(256, reply["width"]!!.jsonPrimitive.int)
        assertEquals(144, reply["height"]!!.jsonPrimitive.int)
        assertArrayEquals(jpeg, Base64.getDecoder().decode(reply["jpeg"]!!.jsonPrimitive.content))
    }

    @Test
    fun `a status reply has no frame`() {
        val request = CastPreviewProtocol.decodeRequest(receiverRequest)!!
        val reply = Json.parseToJsonElement(CastPreviewProtocol.statusMessage(request, CastPreviewProtocol.STATUS_NONE)).jsonObject
        assertEquals("none", reply["status"]!!.jsonPrimitive.content)
        assertEquals(12, reply["step"]!!.jsonPrimitive.int)
        assertFalse(reply.containsKey("jpeg"))
        assertFalse(reply.containsKey("width"))
    }

    @Test
    fun `base64 length`() {
        assertEquals(0, CastPreviewProtocol.base64Length(0))
        assertEquals(4, CastPreviewProtocol.base64Length(1))
        assertEquals(4, CastPreviewProtocol.base64Length(3))
        assertEquals(8, CastPreviewProtocol.base64Length(4))
        assertEquals(Base64.getEncoder().encodeToString(ByteArray(30_000)).length, CastPreviewProtocol.base64Length(30_000))
    }

    @Test
    fun `lowers the JPEG quality until the frame fits the message budget`() {
        val tried = mutableListOf<Int>()
        // 40 000 base64 chars hold 30 000 bytes
        val sizes = mapOf(80 to 45_000, 65 to 31_000, 50 to 29_000, 35 to 20_000)
        val jpeg = CastPreviewProtocol.encodeWithinBudget { quality ->
            tried += quality
            ByteArray(sizes.getValue(quality))
        }
        assertEquals(listOf(80, 65, 50), tried)
        assertEquals(29_000, jpeg!!.size)
        assertTrue(CastPreviewProtocol.base64Length(jpeg.size) <= CastPreviewProtocol.MAX_FRAME_BASE64_CHARS)
    }

    @Test
    fun `keeps the best quality when it fits, and gives up when nothing does`() {
        assertEquals(10_000, CastPreviewProtocol.encodeWithinBudget { ByteArray(10_000) }!!.size)
        assertNull(CastPreviewProtocol.encodeWithinBudget { ByteArray(40_000) })
        assertNull(CastPreviewProtocol.encodeWithinBudget { null })
    }

    @Test
    fun `a reply that wouldn't fit a cast message isn't made`() {
        val request = CastPreviewProtocol.decodeRequest(receiverRequest)!!
        val frame = PreviewJpeg(ByteArray(30_000), 256, 144)
        val fits = CastPreviewProtocol.frameMessage(request, frame)!!
        assertTrue(fits.length <= CastPreviewProtocol.MAX_MESSAGE_CHARS)
        // A huge stream id (its URL) on top of a full frame
        val huge = request.copy(sourceId = "x".repeat(25_000))
        assertNull(CastPreviewProtocol.frameMessage(huge, frame))
    }

    @Test
    fun `frames are 256 px on their longest side`() {
        assertEquals(256 to 144, previewFrameSize(16f / 9f))
        assertEquals(256 to 192, previewFrameSize(4f / 3f))
        assertEquals(144 to 256, previewFrameSize(9f / 16f))
        assertEquals(256 to 144, previewFrameSize(Float.NaN))
        assertEquals(256 to 144, previewFrameSize(0f))
        assertEquals(256 to 1, previewFrameSize(10_000f))
    }

    @Test
    fun `previews need the setting, the receiver's capability and Wi-Fi when asked for`() {
        fun allowed(
            settingOn: Boolean = true,
            onlyOnWifi: Boolean = false,
            onWifi: Boolean = false,
            receiverCapable: Boolean = true,
            localFile: Boolean = false,
        ) = castSeekPreviewAllowed(settingOn, onlyOnWifi, onWifi, receiverCapable, localFile)

        assertTrue(allowed())
        assertFalse("setting off", allowed(settingOn = false))
        assertFalse("setting off, even for a download", allowed(settingOn = false, localFile = true))
        assertFalse("receiver without the capability", allowed(receiverCapable = false))
        assertFalse("Wi-Fi only, on mobile data", allowed(onlyOnWifi = true, onWifi = false))
        assertTrue("Wi-Fi only, on Wi-Fi", allowed(onlyOnWifi = true, onWifi = true))
        assertTrue("Wi-Fi only doesn't apply to a download", allowed(onlyOnWifi = true, onWifi = false, localFile = true))
    }
}
