package com.sf.tadami.data.download

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EpisodeMetadataTest {

    private val metadata = EpisodeMetadata(
        version = EpisodeMetadata.VERSION,
        fullName = "AnsEmbed - 1080p",
        server = "AnsEmbed",
        quality = "1080p",
        video = MediaFileMetadata("Episode 1 (0a1b2c3d).ts", "ts"),
        audios = listOf(MediaFileMetadata("Episode 1 (0a1b2c3d).audio.Japanese.ts", "ts")),
    )

    @Test
    fun theSidecarRoundTrips() {
        val text = EpisodeMetadata.encode(metadata)
        assertEquals(metadata, EpisodeMetadata.decode(text))
        assertEquals(EpisodeMetadata.VERSION, EpisodeMetadata.decode(text)?.version)
    }

    @Test
    fun unknownFieldsAreIgnoredAndMissingOnesDefault() {
        val text = """{"version":1,"fullName":"Voe - MP4","video":{"name":"E.mp4","container":"mp4"},"later":true}"""
        assertEquals(
            EpisodeMetadata(version = 1, fullName = "Voe - MP4", video = MediaFileMetadata("E.mp4", "mp4")),
            EpisodeMetadata.decode(text),
        )
    }

    @Test
    fun anUnreadableOrNewerSidecarIsIgnored() {
        assertNull(EpisodeMetadata.decode("not json"))
        assertNull(EpisodeMetadata.decode("""{"version":1}"""))
        // Always written: a sidecar without it isn't ours
        assertNull(EpisodeMetadata.decode("""{"fullName":"Voe - MP4","video":{"name":"E.mp4","container":"mp4"}}"""))
        assertNull(EpisodeMetadata.decode(EpisodeMetadata.encode(metadata.copy(version = EpisodeMetadata.VERSION + 1))))
    }

    @Test
    fun theLocalStreamIsNamedAfterTheDownloadedOne() {
        assertEquals("AnsEmbed - 1080p", LocalEpisode.streamName(metadata, "Downloaded"))
        // Downloads made before the sidecar, or a stream without a name
        assertEquals("Downloaded", LocalEpisode.streamName(null, "Downloaded"))
        assertEquals("Downloaded", LocalEpisode.streamName(metadata.copy(fullName = " "), "Downloaded"))
    }
}
