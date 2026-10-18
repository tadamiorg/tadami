package com.sf.tadami.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

class SolvingHostsTest {

    private val hosts = SolvingHosts()

    // OkHttp reads the public suffix list from the Android assets, out of reach of unit tests
    private fun site(host: String) = host.split('.').takeLast(2).joinToString(".")

    @Test
    fun aHostIsInTheSetWhileItsChallengeIsSolved() {
        val during = hosts.solving(listOf("animepahe.com", "Animepahe.PW")) { hosts.hosts.value }

        assertEquals(setOf("animepahe.com", "animepahe.pw"), during)
        assertEquals(emptySet<String>(), hosts.hosts.value)
    }

    @Test
    fun concurrentSolvesOfAHostAreCounted() {
        hosts.solving(listOf("example.com")) {
            hosts.solving(listOf("example.com", "other.org")) {
                assertEquals(setOf("example.com", "other.org"), hosts.hosts.value)
            }
            // The first solve still runs
            assertEquals(setOf("example.com"), hosts.hosts.value)
        }
        assertEquals(emptySet<String>(), hosts.hosts.value)
    }

    @Test
    fun theHostLeavesTheSetWhenTheSolveFails() {
        try {
            hosts.solving(listOf("example.com")) { throw IOException("challenge failed") }
            fail()
        } catch (e: IOException) {
            assertEquals("challenge failed", e.message)
        }
        assertEquals(emptySet<String>(), hosts.hosts.value)
    }

    @Test
    fun theSameHostTwiceInOneSolveCountsOnce() {
        hosts.solving(listOf("example.com", "EXAMPLE.com")) {}
        assertEquals(emptySet<String>(), hosts.hosts.value)
    }

    @Test
    fun aSourceWaitsOnItsHostOrAHostOfItsSite() {
        val solving = setOf("www.animepahe.pw", "animepahe.com")

        assertTrue(SolvingHosts.isSolving("https://animepahe.com", solving, ::site))
        assertTrue(SolvingHosts.isSolving("https://animepahe.pw/", solving, ::site))
        assertTrue(SolvingHosts.isSolving("https://www.animepahe.com/api", solving, ::site))
        assertFalse(SolvingHosts.isSolving("https://anime-sama.fr", solving, ::site))
        assertFalse(SolvingHosts.isSolving(null, solving, ::site))
        assertFalse(SolvingHosts.isSolving("not a url", solving, ::site))
        assertFalse(SolvingHosts.isSolving("https://animepahe.com", emptySet(), ::site))
    }
}
