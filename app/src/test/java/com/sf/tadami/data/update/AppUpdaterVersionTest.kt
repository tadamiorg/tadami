package com.sf.tadami.data.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdaterVersionTest {

    @Test
    fun comparesSegmentByNumber() {
        assertTrue(isNewerVersion("v1.10.0", "1.9.3"))
        assertTrue(isNewerVersion("v1.9.4", "1.9.3"))
        assertFalse(isNewerVersion("v1.9.3", "1.9.3"))
        assertFalse(isNewerVersion("v1.9.2", "1.9.3"))
    }

    @Test
    fun missingSegmentsCountAsZero() {
        assertTrue(isNewerVersion("v1.9.3.1", "1.9.3"))
        assertFalse(isNewerVersion("v1.10", "1.10.0"))
        assertFalse(isNewerVersion("v1.10.0", "1.10"))
    }

    @Test
    fun releaseReplacesItsPreRelease() {
        assertTrue(isNewerVersion("v2.0.0", "2.0.0-beta1"))
        assertTrue(isNewerVersion("v2.0.0", "2.0.0-rc10"))
        assertTrue(isNewerVersion("v2.0.1", "2.0.0-beta1"))
        assertFalse(isNewerVersion("v1.9.9", "2.0.0-beta1"))
        assertFalse(isNewerVersion("v2.0.0-beta1", "2.0.0"))
        assertFalse(isNewerVersion("v2.0.0-beta2", "2.0.0-beta1"))
    }
}
