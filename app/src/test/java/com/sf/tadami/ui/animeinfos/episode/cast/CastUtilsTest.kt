package com.sf.tadami.ui.animeinfos.episode.cast

import com.google.android.gms.cast.MediaStatus
import com.google.android.gms.cast.MediaTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CastUtilsTest {

    @Test
    fun onlyAMediaPlayedToItsEndIsFinished() {
        assertTrue(isCastMediaFinished(MediaStatus.IDLE_REASON_FINISHED))
        assertFalse(isCastMediaFinished(MediaStatus.IDLE_REASON_ERROR))
        assertFalse(isCastMediaFinished(MediaStatus.IDLE_REASON_CANCELED))
        assertFalse(isCastMediaFinished(null))
    }

    @Test
    fun aFailedMediaHasEndedButNotFinished() {
        assertTrue(isCastMediaEnded(MediaStatus.IDLE_REASON_ERROR))
        assertTrue(isCastMediaEnded(MediaStatus.IDLE_REASON_FINISHED))
        assertFalse(isCastMediaEnded(MediaStatus.IDLE_REASON_INTERRUPTED))
    }

    @Test
    fun vpnAndCellularInterfacesAreNotLanCandidates() {
        listOf("tun0", "ppp0", "wg0", "ipsec0", "rmnet_data0").forEach { assertFalse(it, isLanInterfaceCandidate(it)) }
        listOf("wlan0", "eth0", "ap0", "swlan0").forEach { assertTrue(it, isLanInterfaceCandidate(it)) }
    }

    @Test
    fun theActiveSubtitleIsTheFirstActiveTextTrack() {
        val tracks = listOf(
            MediaTrack.Builder(1L, MediaTrack.TYPE_TEXT).build(),
            MediaTrack.Builder(2L, MediaTrack.TYPE_TEXT).build(),
            MediaTrack.Builder(3L, MediaTrack.TYPE_AUDIO).build(),
        )
        assertEquals(2L, activeCastSubtitleTrackId(longArrayOf(3L, 2L), tracks))
        assertNull(activeCastSubtitleTrackId(longArrayOf(3L), tracks))
        assertNull(activeCastSubtitleTrackId(longArrayOf(1L), null))
        assertNull(activeCastSubtitleTrackId(null, tracks))
    }
}
