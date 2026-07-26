package com.sf.tadami.ui.animeinfos.episode.cast.channels

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Wire-contract test for the web receiver's handshake reply (same Json config as HandshakeChannel). */
class ReceiverHandshakeMessageTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Test
    fun `parses a full reply`() {
        val msg = json.decodeFromString<ReceiverHandshakeMessage>(
            """{"receiverType":"web","receiverProtocol":2,"receiverVersion":"1.0.0"}"""
        )
        assertEquals("web", msg.receiverType)
        assertEquals(2, msg.receiverProtocol)
        assertEquals("1.0.0", msg.receiverVersion)
    }

    @Test
    fun `ignores unknown keys and missing fields`() {
        val msg = json.decodeFromString<ReceiverHandshakeMessage>(
            """{"receiverType":"web","futureField":{"nested":true}}"""
        )
        assertEquals("web", msg.receiverType)
        assertEquals(0, msg.receiverProtocol)
        assertEquals("", msg.receiverVersion)
    }

    @Test
    fun `garbage input fails without crashing runCatching callers`() {
        val result = runCatching { json.decodeFromString<ReceiverHandshakeMessage>("not json at all") }
        assertTrue(result.isFailure)
    }
}
