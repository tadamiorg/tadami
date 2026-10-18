package com.sf.tadami.ui.webview

import com.sf.tadami.ui.webview.NavigationDecision.ALLOW
import com.sf.tadami.ui.webview.NavigationDecision.BLOCK
import com.sf.tadami.ui.webview.NavigationDecision.POPUP
import okhttp3.HttpUrl
import org.junit.Assert.assertEquals
import org.junit.Test

class WebViewNavigationPolicyTest {

    private val page = "https://voir-anime.to/anime/100-meters/film-vostfr-100-meters/"

    // OkHttp reads the public suffix list from the Android assets, out of reach of unit tests
    private val publicSuffixes = setOf("to", "com", "org", "net", "io", "uk", "co.uk", "github.io")

    private fun site(url: HttpUrl): String {
        val labels = url.host.split('.')
        val suffixLength = (labels.size - 1 downTo 1).firstOrNull { length ->
            labels.takeLast(length).joinToString(".") in publicSuffixes
        } ?: return url.host
        return labels.takeLast(suffixLength + 1).joinToString(".")
    }

    private fun decide(
        target: String,
        current: String? = page,
        isRedirect: Boolean = false,
        initialLoadDone: Boolean = true,
        isPopup: Boolean = false,
        canOpenPopup: Boolean = true,
    ) = WebViewNavigationPolicy.decide(
        currentUrl = current,
        targetUrl = target,
        isRedirect = isRedirect,
        initialLoadDone = initialLoadDone,
        isPopup = isPopup,
        canOpenPopup = canOpenPopup,
        isAdHost = { it == "ads.example.com" },
        siteOf = ::site,
    )

    @Test
    fun crossSiteNavigationOfThePageOpensAPopup() {
        assertEquals(POPUP, decide("https://cornyanticipatedfragile.com/isfydgn1j?key=1"))
        assertEquals(POPUP, decide("http://127.0.0.1:8080/ad.html", current = "http://localhost:8080/main.html"))
    }

    @Test
    fun sameSiteNavigationIsAllowed() {
        assertEquals(ALLOW, decide("https://voir-anime.to/anime/100-meters/"))
        assertEquals(ALLOW, decide("https://www.voir-anime.to/"))
        assertEquals(ALLOW, decide("http://voir-anime.to/?s=meters"))
        assertEquals(ALLOW, decide("http://localhost:8080/page2.html", current = "http://localhost:8080/main.html"))
    }

    @Test
    fun registrableDomainsComparePublicSuffixesAsAWhole() {
        // github.io is a public suffix: two of its sites are different sites
        assertEquals(POPUP, decide("https://ads.github.io/", current = "https://source.github.io/"))
        assertEquals(ALLOW, decide("https://a.example.co.uk/", current = "https://b.example.co.uk/"))
        assertEquals(POPUP, decide("https://other.co.uk/", current = "https://b.example.co.uk/"))
    }

    @Test
    fun initialLoadAndItsRedirectsAreAllowed() {
        assertEquals(ALLOW, decide("https://mirror.example.net/", initialLoadDone = false))
        assertEquals(ALLOW, decide("https://mirror.example.net/", current = null, initialLoadDone = false))
    }

    @Test
    fun serverRedirectsOfAnAllowedNavigationAreAllowed() {
        assertEquals(ALLOW, decide("https://mirror.example.net/", isRedirect = true))
    }

    @Test
    fun popupsBrowseAnySite() {
        assertEquals(ALLOW, decide("https://example.org/", current = "https://example.com/", isPopup = true))
        assertEquals(ALLOW, decide("https://example.org/", current = null, isPopup = true, canOpenPopup = false))
    }

    @Test
    fun adHostsAreBlockedEverywhere() {
        assertEquals(BLOCK, decide("https://ads.example.com/click"))
        assertEquals(BLOCK, decide("https://ads.example.com/click", isRedirect = true))
        assertEquals(BLOCK, decide("https://ads.example.com/click", initialLoadDone = false))
        assertEquals(BLOCK, decide("https://ads.example.com/click", isPopup = true))
        assertEquals(BLOCK, decide("https://ads.example.com/click", isPopup = true, current = null))
    }

    @Test
    fun crossSiteNavigationIsDroppedOnceThePopupStackIsFull() {
        assertEquals(BLOCK, decide("https://example.org/", canOpenPopup = false))
        assertEquals(ALLOW, decide("https://voir-anime.to/", canOpenPopup = false))
    }

    @Test
    fun unknownPageAllowsTheNavigation() {
        assertEquals(ALLOW, decide("https://example.org/", current = null))
        assertEquals(ALLOW, decide("https://example.org/", current = "about:blank"))
    }

    @Test
    fun nonHttpTargetsAreBlocked() {
        assertEquals(BLOCK, decide("intent://scan/#Intent;scheme=zxing;end"))
        assertEquals(BLOCK, decide("aliexpress://home?fromPageCode=home", isPopup = true))
    }
}
