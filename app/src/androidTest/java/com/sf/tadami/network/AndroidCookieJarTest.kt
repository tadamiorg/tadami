package com.sf.tadami.network

import android.webkit.CookieManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class AndroidCookieJarTest {

    private lateinit var jar: AndroidCookieJar
    private val manager: CookieManager get() = CookieManager.getInstance()

    @Before
    fun setUp() {
        runOnMain { jar = AndroidCookieJar() }
        clearAllCookies()
    }

    @After
    fun tearDown() {
        clearAllCookies()
    }

    @Test
    fun removeDropsNamedCookieWhateverItsPosition() {
        val url = "https://example.org/".toHttpUrl()
        manager.setCookie(url.toString(), "a=1; Path=/")
        manager.setCookie(url.toString(), "cf_clearance=token; Path=/")
        manager.setCookie(url.toString(), "b=2; Path=/")

        val removed = jar.remove(url, listOf("cf_clearance"), 0)

        assertEquals(1, removed)
        val names = jar.get(url).map { it.name }
        assertFalse("cf_clearance must be gone, got $names", "cf_clearance" in names)
        assertTrue("other cookies must survive, got $names", names.containsAll(listOf("a", "b")))
    }

    @Test
    fun removeDropsDomainCookies() {
        val url = "https://www.example.org/anime".toHttpUrl()
        // Cloudflare sets cf_clearance on the registrable domain, not the host
        manager.setCookie(url.toString(), "cf_clearance=token; Domain=.example.org; Path=/")
        manager.setCookie(url.toString(), "session=abc; Path=/")
        assertTrue(jar.get(url).any { it.name == "cf_clearance" })

        jar.remove(url)

        assertTrue("all cookies must be gone, got ${jar.get(url)}", jar.get(url).isEmpty())
    }

    @Test
    fun getParsesEveryCookie() {
        val url = "https://example.org/".toHttpUrl()
        manager.setCookie(url.toString(), "a=1; Path=/")
        manager.setCookie(url.toString(), "b=2; Path=/")

        val cookies = jar.get(url).associate { it.name to it.value }

        assertEquals(mapOf("a" to "1", "b" to "2"), cookies)
    }

    private fun clearAllCookies() {
        val latch = CountDownLatch(1)
        runOnMain { manager.removeAllCookies { latch.countDown() } }
        latch.await(5, TimeUnit.SECONDS)
    }

    private fun runOnMain(block: () -> Unit) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(block)
    }
}
