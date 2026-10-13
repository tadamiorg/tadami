package com.sf.tadami.domain.episode

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EpisodeWatchedTest {

    @Test
    fun aFullyWatchedEpisodeReachesTheHundredPercentThreshold() {
        // The player caps the position at the duration, so 100% is only ever reached exactly
        assertTrue(isEpisodeWatched(timeSeen = 1_440_000, totalTime = 1_440_000, threshold = 100))
    }

    @Test
    fun theThresholdItselfCountsAsWatched() {
        assertTrue(isEpisodeWatched(timeSeen = 850, totalTime = 1_000, threshold = 85))
    }

    @Test
    fun belowTheThresholdIsNotWatched() {
        assertFalse(isEpisodeWatched(timeSeen = 849, totalTime = 1_000, threshold = 85))
        assertFalse(isEpisodeWatched(timeSeen = 1_439_999, totalTime = 1_440_000, threshold = 100))
    }
}
