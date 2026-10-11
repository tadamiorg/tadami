package com.sf.tadami.network.utils

import com.sf.tadami.network.NetworkHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UserAgentsTest {

    @Test
    fun desktopChromeUsesTheGivenMajor() {
        val ua = UserAgents.desktopChrome(153)
        assertEquals(153, UserAgents.chromeMajor(ua))
        assertTrue(ua.startsWith("Mozilla/5.0 (Windows NT 10.0; Win64; x64)"))
    }

    @Test
    fun mobileChromeIsChromeForAndroidReducedAgent() {
        val ua = UserAgents.mobileChrome(153)
        assertEquals(153, UserAgents.chromeMajor(ua))
        assertEquals(
            "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/153.0.0.0 Mobile Safari/537.36",
            ua,
        )
        val hints = UserAgents.clientHintsFor(ua)!!
        assertEquals("Android", hints.platform)
        assertTrue(hints.mobile)
        assertTrue(hints.brands.any { it.name == "Google Chrome" && it.majorVersion == "153" })
    }

    @Test
    fun constantDefaultMatchesTheFallbackVersion() {
        assertEquals(UserAgents.mobileChrome(UserAgents.FALLBACK_CHROME_MAJOR), NetworkHelper.DEFAULT_USER_AGENT)
        assertFalse(UserAgents.isLegacyDefault(NetworkHelper.DEFAULT_USER_AGENT))
    }

    @Test
    fun majorFromWebViewVersionName() {
        assertEquals(153, UserAgents.majorFromVersionName("153.0.8010.57"))
        assertEquals(120, UserAgents.majorFromVersionName("120"))
        assertNull(UserAgents.majorFromVersionName(null))
        assertNull(UserAgents.majorFromVersionName(""))
        assertNull(UserAgents.majorFromVersionName("beta"))
        assertNull(UserAgents.majorFromVersionName("0.1"))
    }

    @Test
    fun chromiumMajorOnlyFromChromiumNumberedProviders() {
        assertEquals(153, UserAgents.chromiumMajorFromPackage("com.google.android.webview", "153.0.8010.57"))
        assertEquals(150, UserAgents.chromiumMajorFromPackage("com.google.android.webview.beta", "150.0.1.2"))
        assertEquals(149, UserAgents.chromiumMajorFromPackage("com.android.chrome", "149.0.7000.10"))
        assertEquals(148, UserAgents.chromiumMajorFromPackage("org.chromium.webview_shell", "148.0.1"))
        // Huawei numbers its WebView independently of Chromium
        assertNull(UserAgents.chromiumMajorFromPackage("com.huawei.webview", "15.0.4.324"))
        assertNull(UserAgents.chromiumMajorFromPackage("com.example.webview", "153.0.0.0"))
        assertNull(UserAgents.chromiumMajorFromPackage("com.android.webview", "12.0.0"))
        assertNull(UserAgents.chromiumMajorFromPackage(null, "153.0.0.0"))
        assertNull(UserAgents.chromiumMajorFromPackage("com.google.android.webview", null))
    }

    @Test
    fun chromeBrandsMatchWhatChromeSends() {
        fun header(major: Int) = UserAgents.chromeBrands(major).joinToString { "\"${it.name}\";v=\"${it.majorVersion}\"" }

        assertEquals("\"Not_A Brand\";v=\"8\", \"Chromium\";v=\"120\", \"Google Chrome\";v=\"120\"", header(120))
        assertEquals("\"Chromium\";v=\"124\", \"Google Chrome\";v=\"124\", \"Not-A.Brand\";v=\"99\"", header(124))
        assertEquals("\"Google Chrome\";v=\"131\", \"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\"", header(131))
        assertEquals("\"Chromium\";v=\"136\", \"Google Chrome\";v=\"136\", \"Not.A/Brand\";v=\"99\"", header(136))
        assertEquals("\"Google Chrome\";v=\"141\", \"Not?A_Brand\";v=\"8\", \"Chromium\";v=\"141\"", header(141))
    }

    @Test
    fun legacyDefaultsAreRecognised() {
        UserAgents.LEGACY_DEFAULTS.forEach { assertTrue(UserAgents.isLegacyDefault(it)) }
        assertTrue(UserAgents.isLegacyDefault(" ${UserAgents.LEGACY_DEFAULTS.first()} "))
        assertFalse(UserAgents.isLegacyDefault(UserAgents.desktopChrome(153)))
        assertFalse(UserAgents.isLegacyDefault(null))
    }

    @Test
    fun clientHintsForDesktopChrome() {
        val hints = UserAgents.clientHintsFor(UserAgents.desktopChrome(150))!!

        assertEquals("Windows", hints.platform)
        assertFalse(hints.mobile)
        assertEquals("150.0.0.0", hints.fullVersion)
        assertEquals("x86", hints.architecture)
        assertEquals(64, hints.bitness)
        assertTrue(hints.brands.any { it.name == "Google Chrome" && it.majorVersion == "150" })
        assertTrue(hints.brands.none { it.name == "Android WebView" })
    }

    @Test
    fun clientHintsForMobileChrome() {
        val ua = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/151.0.0.0 Mobile Safari/537.36"
        val hints = UserAgents.clientHintsFor(ua)!!

        assertEquals("Android", hints.platform)
        assertEquals("14", hints.platformVersion)
        assertTrue(hints.mobile)
        assertEquals("", hints.architecture)
    }

    @Test
    fun noClientHintsForNonChromiumAgents() {
        assertNull(UserAgents.clientHintsFor("Mozilla/5.0 (Android 14; Mobile; rv:133.0) Gecko/133.0 Firefox/133.0"))
        assertNull(UserAgents.clientHintsFor(""))
    }
}
