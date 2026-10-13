package com.sf.tadami.ui.animeinfos.episode.cast

import com.google.android.gms.cast.MediaStatus
import org.junit.Assert.assertFalse
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
}
