package com.sf.tadami.ui.webview

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HostBlocklistTest {

    private val list = HostBlocklist.parse(
        sequenceOf(
            "# comment",
            "0.0.0.0 ads.example.com\r",
            "0.0.0.0\ttracker.example.org   # trailing comment",
            "bare.example.net",
            "",
            "0.0.0.0 challenges.cloudflare.com",
        ),
    )

    @Test
    fun parsesHostsFileLines() {
        assertEquals(4, list.size)
        assertTrue(list.isBlocked("ads.example.com"))
        assertTrue(list.isBlocked("tracker.example.org"))
        assertTrue(list.isBlocked("bare.example.net"))
    }

    @Test
    fun matchesExactHostsOnly() {
        // the previous substring scan blocked any host that was a prefix of a listed one
        assertFalse(list.isBlocked("ads.example.co"))
        assertFalse(list.isBlocked("example.com"))
        assertFalse(list.isBlocked("sub.ads.example.com"))
    }

    @Test
    fun isCaseAndTrailingDotInsensitive() {
        assertTrue(list.isBlocked("ADS.Example.com"))
        assertTrue(list.isBlocked("ads.example.com."))
    }

    @Test
    fun neverBlocksChallengeProviders() {
        assertFalse(list.isBlocked("challenges.cloudflare.com"))
        assertFalse(list.isBlocked("newassets.hcaptcha.com"))
    }

    @Test
    fun emptyOrNullHostIsNotBlocked() {
        assertFalse(list.isBlocked(null))
        assertFalse(list.isBlocked(""))
        assertFalse(HostBlocklist.EMPTY.isBlocked("ads.example.com"))
    }
}
