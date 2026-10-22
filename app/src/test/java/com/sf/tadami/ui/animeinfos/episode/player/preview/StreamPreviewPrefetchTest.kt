package com.sf.tadami.ui.animeinfos.episode.player.preview

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import com.sf.tadami.preferences.player.PlayerPreferences
import com.sf.tadami.ui.animeinfos.episode.player.preview.PreviewPrefetch.Playback
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The phone player's prefetch of a streamed episode: when it may load, its setting, its pace. */
class StreamPreviewPrefetchTest {

    private fun mayLoad(
        localFile: Boolean = false,
        onlyOnWifi: Boolean = false,
        onWifi: Boolean = false,
        mainCanSpareNetwork: Boolean = true,
    ) = previewPrefetchMayLoad(localFile, onlyOnWifi, onWifi, mainCanSpareNetwork)

    @Test
    fun `with Only on Wi-Fi off, a stream is prefetched on mobile data too`() {
        assertTrue(mayLoad(onlyOnWifi = false, onWifi = false))
        assertTrue(mayLoad(onlyOnWifi = false, onWifi = true))
    }

    @Test
    fun `with Only on Wi-Fi on, a stream is prefetched on Wi-Fi only`() {
        assertFalse(mayLoad(onlyOnWifi = true, onWifi = false))
        assertTrue(mayLoad(onlyOnWifi = true, onWifi = true))
    }

    @Test
    fun `a stream waits while the main player can't spare the network`() {
        // Rebuffering, or less than 10 s buffered ahead
        assertFalse(mayLoad(onWifi = true, mainCanSpareNetwork = mainPlayerCanSpareNetwork(true, 60_000, 0, 1_000_000)))
        assertFalse(mayLoad(onWifi = true, mainCanSpareNetwork = mainPlayerCanSpareNetwork(false, 9_999, 0, 1_000_000)))
        assertTrue(mayLoad(onWifi = true, mainCanSpareNetwork = mainPlayerCanSpareNetwork(false, 10_000, 0, 1_000_000)))
        // The whole video buffered
        assertTrue(mayLoad(onWifi = true, mainCanSpareNetwork = mainPlayerCanSpareNetwork(false, 0, 1_000_000, 1_000_000)))
    }

    @Test
    fun `a downloaded episode needs no network`() {
        assertTrue(mayLoad(localFile = true, onlyOnWifi = true, onWifi = false, mainCanSpareNetwork = false))
    }

    @Test
    fun `the setting and the seekbar preview turn the prefetch on and off, for any stream`() {
        assertTrue(phonePreviewPrefetchOn(seekPreview = true, prefetch = true, hasSource = true))
        assertFalse("prefetch off", phonePreviewPrefetchOn(seekPreview = true, prefetch = false, hasSource = true))
        assertFalse("preview off", phonePreviewPrefetchOn(seekPreview = false, prefetch = true, hasSource = true))
        assertFalse("nothing playing", phonePreviewPrefetchOn(seekPreview = true, prefetch = true, hasSource = false))
    }

    @Test
    fun `one setting for the phone and the cast, its stored choice kept`() {
        assertTrue(PlayerPreferences.transform(emptyPreferences()).seekPreviewPrefetch)
        // Stored under the key of the cast's former setting
        val stored = mutablePreferencesOf(booleanPreferencesKey("player_seek_preview_cast_prefetch") to false)
        assertFalse(PlayerPreferences.transform(stored).seekPreviewPrefetch)
    }

    @Test
    fun `a stream is prefetched more gently than a download`() {
        assertEquals(1_000L, PREVIEW_PREFETCH_GAP_STREAM_MS)
        assertTrue(PREVIEW_PREFETCH_GAP_LOCAL_MS < PREVIEW_PREFETCH_GAP_STREAM_MS)
        var time = 0L
        val schedule = PreviewPrefetch { time }
        schedule.onStatus(Playback.PLAYING, 0)
        time += PreviewPrefetch.STABLE_MS
        schedule.onStatus(Playback.PLAYING, 0)
        schedule.onDecodeEnded(PREVIEW_PREFETCH_GAP_STREAM_MS)
        time += PREVIEW_PREFETCH_GAP_LOCAL_MS
        assertFalse(schedule.ready())
        time += PREVIEW_PREFETCH_GAP_STREAM_MS - PREVIEW_PREFETCH_GAP_LOCAL_MS
        assertTrue(schedule.ready())
    }
}
