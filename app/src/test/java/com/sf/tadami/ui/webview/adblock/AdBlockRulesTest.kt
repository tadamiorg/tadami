package com.sf.tadami.ui.webview.adblock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

class AdBlockRulesTest {

    // OkHttp reads the public suffix list from the Android assets, out of reach of unit tests
    private val publicSuffixes = setOf("com", "net", "org", "to", "ru", "uk", "co.uk", "fr", "de", "github.io")

    private fun site(host: String): String {
        val labels = host.split('.')
        val suffixLength = (labels.size - 1 downTo 1).firstOrNull { length ->
            labels.takeLast(length).joinToString(".") in publicSuffixes
        } ?: return host
        return labels.takeLast(suffixLength + 1).joinToString(".")
    }

    private fun rules(vararg lines: String): AdBlockRules = FilterParser().apply { addAll(lines.asSequence()) }.build(::site)

    private fun AdBlockRules.blocks(
        url: String,
        kind: RequestKind = RequestKind.SUBRESOURCE,
        context: String? = "page.com",
        page: String? = context,
    ): Boolean = isBlocked(AdBlockRequest.of(url, kind, context, page)!!)

    @Test
    fun domainAnchoredRulesMatchTheHostAndItsSubdomains() {
        val rules = rules("||ads.example.com^")
        assertTrue(rules.blocks("https://ads.example.com/banner.js"))
        assertTrue(rules.blocks("https://cdn.ads.example.com/banner.js"))
        assertTrue(rules.blocks("https://ADS.example.com./x"))
        assertFalse(rules.blocks("https://example.com/"))
        assertFalse(rules.blocks("https://notads.example.com/"))
        assertFalse(rules.blocks("https://ads.example.co/"))
    }

    @Test
    fun hostsFileAndBareHostLinesAreDomainRules() {
        val rules = rules(
            "# comment",
            "0.0.0.0 tracker.example.org   # trailing comment",
            "127.0.0.1\tads.example.net",
            "bare.example.com",
            "0.0.0.0 localhost",
            "0.0.0.0 0.0.0.0",
        )
        assertTrue(rules.blocks("https://tracker.example.org/"))
        assertTrue(rules.blocks("https://sub.ads.example.net/"))
        assertTrue(rules.blocks("http://bare.example.com/"))
        assertFalse(rules.blocks("http://localhost/"))
        assertEquals(3, rules.blocks.size)
    }

    @Test
    fun pathsMatchAfterTheHost() {
        val rules = rules(
            "||cdn.example.com/ads/",
            "||cdn.example.com^*/pop.js|",
            "||player.example.com/vast^",
        )
        assertTrue(rules.blocks("https://cdn.example.com/ads/banner.png"))
        assertFalse(rules.blocks("https://cdn.example.com/video/ads/banner.png"))
        assertTrue(rules.blocks("https://cdn.example.com/a/b/pop.js"))
        assertFalse(rules.blocks("https://cdn.example.com/a/b/pop.js?x=1"))
        assertTrue(rules.blocks("https://player.example.com/vast?id=1"))
        assertTrue(rules.blocks("https://player.example.com/vast"))
        assertFalse(rules.blocks("https://player.example.com/vastly"))
        assertFalse(rules.blocks("https://player.example.com/video.m3u8"))
    }

    @Test
    fun exceptionsWinOverBlocks() {
        val rules = rules(
            "||example.com^",
            "@@||cdn.example.com^",
            "@@||example.com/player/",
        )
        assertTrue(rules.blocks("https://ads.example.com/"))
        assertFalse(rules.blocks("https://cdn.example.com/lib.js"))
        assertFalse(rules.blocks("https://example.com/player/embed"))
        assertTrue(rules.blocks("https://example.com/ads"))
    }

    @Test
    fun importantBlocksWinOverExceptions() {
        val rules = rules(
            "||tracker.example.com^\$important",
            "@@||tracker.example.com^",
        )
        assertTrue(rules.blocks("https://tracker.example.com/"))
    }

    @Test
    fun thirdPartyRulesComparePagesAndRequestsBySite() {
        val rules = rules(
            "||widget.example.com^\$third-party",
            "||self.example.net^\$~third-party",
        )
        assertTrue(rules.blocks("https://widget.example.com/", context = "page.com"))
        assertFalse(rules.blocks("https://widget.example.com/", context = "www.example.com"))
        assertTrue(rules.blocks("https://self.example.net/", context = "www.example.net"))
        assertFalse(rules.blocks("https://self.example.net/", context = "page.com"))
        // Unknown page: a block that depends on it doesn't apply
        assertFalse(rules.blocks("https://widget.example.com/", context = null))
    }

    @Test
    fun uBlockPartyAliasesAreUnderstood() {
        val rules = rules("||a.example.com^\$3p", "||b.example.com^\$1p")
        assertTrue(rules.blocks("https://a.example.com/", context = "page.com"))
        assertFalse(rules.blocks("https://b.example.com/", context = "page.com"))
        assertTrue(rules.blocks("https://b.example.com/", context = "example.com"))
    }

    @Test
    fun exceptionsThatDependOnAnUnknownPageApply() {
        val rules = rules(
            "||ads.example.com^",
            "@@||ads.example.com^\$third-party",
        )
        assertFalse(rules.blocks("https://ads.example.com/", context = null))
        assertTrue(rules.blocks("https://ads.example.com/", context = "example.com"))
    }

    @Test
    fun popupRulesOnlyApplyToPopups() {
        val rules = rules("||popunder.example.com^\$popup")
        assertTrue(rules.blocks("https://popunder.example.com/go", RequestKind.POPUP))
        assertFalse(rules.blocks("https://popunder.example.com/go", RequestKind.DOCUMENT))
        assertFalse(rules.blocks("https://popunder.example.com/go.js", RequestKind.SUBRESOURCE))
    }

    @Test
    fun plainRulesApplyToEveryKind() {
        val rules = rules("||ads.example.com^")
        RequestKind.entries.forEach { kind -> assertTrue(kind.name, rules.blocks("https://ads.example.com/", kind)) }
    }

    @Test
    fun typeOptionsMapToTheKindsTheWebViewTells() {
        val rules = rules(
            "||scripts.example.com^\$script",
            "||scam.example.com^\$document",
            "||everything.example.com^\$all",
            "||not-images.example.com^\$~image",
        )
        assertTrue(rules.blocks("https://scripts.example.com/a.js"))
        assertFalse(rules.blocks("https://scripts.example.com/", RequestKind.DOCUMENT))
        assertFalse(rules.blocks("https://scripts.example.com/", RequestKind.POPUP))
        assertTrue(rules.blocks("https://scam.example.com/", RequestKind.DOCUMENT))
        assertFalse(rules.blocks("https://scam.example.com/x.js"))
        RequestKind.entries.forEach { kind -> assertTrue(rules.blocks("https://everything.example.com/", kind)) }
        assertTrue(rules.blocks("https://not-images.example.com/a.js"))
        assertFalse(rules.blocks("https://not-images.example.com/", RequestKind.DOCUMENT))
    }

    @Test
    fun domainOptionLimitsTheRuleToSomePages() {
        val rules = rules(
            "||cdn.example.com^\$domain=site.com|~safe.site.com",
            "||widget.example.org^\$domain=~friend.com",
            "||ads.example.net^\$domain=shop.*",
        )
        assertTrue(rules.blocks("https://cdn.example.com/", context = "site.com"))
        assertTrue(rules.blocks("https://cdn.example.com/", context = "www.site.com"))
        assertFalse(rules.blocks("https://cdn.example.com/", context = "safe.site.com"))
        assertFalse(rules.blocks("https://cdn.example.com/", context = "other.com"))
        assertFalse(rules.blocks("https://cdn.example.com/", context = null))

        assertTrue(rules.blocks("https://widget.example.org/", context = "other.com"))
        assertFalse(rules.blocks("https://widget.example.org/", context = "www.friend.com"))

        assertTrue(rules.blocks("https://ads.example.net/", context = "www.shop.co.uk"))
        assertTrue(rules.blocks("https://ads.example.net/", context = "shop.de"))
        assertFalse(rules.blocks("https://ads.example.net/", context = "workshop.de"))
    }

    @Test
    fun documentExceptionsAllowAPageAndWhatItLoads() {
        val rules = rules(
            "||ads.example.com^",
            "||optout.example.org^",
            "@@||optout.example.org^\$document",
        )
        assertFalse(rules.blocks("https://optout.example.org/", RequestKind.DOCUMENT))
        assertFalse(rules.blocks("https://ads.example.com/a.js", context = "optout.example.org"))
        assertTrue(rules.blocks("https://ads.example.com/a.js", context = "page.com"))
    }

    @Test
    fun badfilterCancelsTheSameRule() {
        val rules = rules(
            "||gone.example.com^",
            "||gone.example.com^\$badfilter",
            "||kept.example.com^\$popup",
            "||kept.example.com/x\$domain=a.com",
            "||kept.example.com/x\$domain=a.com,badfilter",
        )
        assertFalse(rules.blocks("https://gone.example.com/"))
        assertFalse(rules.blocks("https://kept.example.com/x", context = "a.com"))
        assertTrue(rules.blocks("https://kept.example.com/x", RequestKind.POPUP, context = "a.com"))
    }

    @Test
    fun unsupportedAndMalformedLinesAreSkipped() {
        val skipped = listOf(
            "! comment",
            "[Adblock Plus 2.0]",
            "example.com##.ad",
            "example.com#@#.ad",
            "example.com#?#div:has(.ad)",
            "example.com##+js(set-constant, x, 1)",
            "example.com#\$#body { color: red }",
            "example.com#\$?#body { color: red }",
            "example.com\$\$script[src]",
            "/banner/ads/*",
            "-ad-300x250.",
            "|https://example.com/ad^",
            "/^https:\\/\\/[a-z]+\\.com\\/ads/",
            "||ads.example.com^\$redirect=noop.js",
            "||ads.example.com^\$csp=script-src 'none'",
            "||ads.example.com^\$removeparam=utm",
            "||ads.example.com^\$match-case",
            "||ads.example.com^\$domain=/regex/",
            "||ads.example.com^\$unknown-option",
            "@@||ads.example.com^\$generichide",
            "@@||ads.example.com^\$elemhide",
            "@@ads.example.com",
            "||ads.*^\$popup",
            "||*.example.com^",
            "||ads.",
            "||com^",
            "||-ads.example.com^",
            "||ads.example.com|",
            "0.0.0.0",
            "0.0.0.0 a b",
            "",
        )
        skipped.forEach { assertNull(it, FilterParser.parseLine(it)) }
        val rules = rules(*skipped.toTypedArray())
        assertEquals(0, rules.blocks.size)
        assertEquals(0, rules.exceptions.size)
    }

    @Test
    fun anExceptionWithAnUnsupportedOptionDoesntAllowEverything() {
        val rules = rules("||ads.example.com^", "@@||ads.example.com^\$generichide")
        assertTrue(rules.blocks("https://ads.example.com/"))
    }

    @Test
    fun challengeProvidersAreNeverBlocked() {
        val rules = rules("||cloudflare.com^", "||hcaptcha.com^", "0.0.0.0 recaptcha.net")
        assertFalse(rules.blocks("https://challenges.cloudflare.com/turnstile/v0/api.js"))
        assertFalse(rules.blocks("https://newassets.hcaptcha.com/c/x.js"))
        assertFalse(rules.blocks("https://www.recaptcha.net/recaptcha/api.js"))
        assertTrue(rules.blocks("https://cdnjs.cloudflare.com/x.js"))
    }

    @Test
    fun emptyRulesBlockNothing() {
        assertFalse(AdBlockRules.EMPTY.blocks("https://ads.example.com/"))
        assertFalse(rules().blocks("https://ads.example.com/"))
    }

    @Test
    fun pathPatternsHandleWildcardsAndSeparators() {
        assertTrue(AdBlockRules.pathMatches("/ads/", "/ads/x"))
        assertTrue(AdBlockRules.pathMatches("^", "/x"))
        assertTrue(AdBlockRules.pathMatches("^", ""))
        assertTrue(AdBlockRules.pathMatches("/a*b*c|", "/axxbyyc"))
        assertFalse(AdBlockRules.pathMatches("/a*b*c|", "/axxbyycz"))
        assertTrue(AdBlockRules.pathMatches("/x^", "/x?y"))
        assertFalse(AdBlockRules.pathMatches("/x^", "/x-y"))
        assertTrue(AdBlockRules.pathMatches(":8080/", ":8080/a"))
        assertFalse(AdBlockRules.pathMatches("/ads", "/video/ads"))
    }

    @Test
    fun theCompiledFormatRoundTrips() {
        val original = rules(
            "||ads.example.com^",
            "||widget.example.com^\$third-party",
            "||popunder.example.com^\$popup",
            "||cdn.example.com/ads/*.js\$domain=site.com|~safe.site.com|shop.*,important",
            "@@||cdn.example.com/ads/ok.js",
            "@@||optout.example.org^\$document",
            "0.0.0.0 tracker.example.org",
        )
        val bytes = ByteArrayOutputStream().also { out -> DataOutputStream(out).use { original.write(it) } }.toByteArray()
        val read = DataInputStream(ByteArrayInputStream(bytes)).use { AdBlockRules.read(it, ::site) }

        assertEquals(original.blocks.size, read.blocks.size)
        assertEquals(original.exceptions.size, read.exceptions.size)
        val requests = listOf(
            AdBlockRequest.of("https://x.ads.example.com/", RequestKind.SUBRESOURCE, "page.com")!!,
            AdBlockRequest.of("https://widget.example.com/", RequestKind.SUBRESOURCE, "page.com")!!,
            AdBlockRequest.of("https://widget.example.com/", RequestKind.SUBRESOURCE, "example.com")!!,
            AdBlockRequest.of("https://popunder.example.com/", RequestKind.POPUP, "page.com")!!,
            AdBlockRequest.of("https://popunder.example.com/", RequestKind.DOCUMENT, "page.com")!!,
            AdBlockRequest.of("https://cdn.example.com/ads/a.js", RequestKind.SUBRESOURCE, "site.com")!!,
            AdBlockRequest.of("https://cdn.example.com/ads/a.js", RequestKind.SUBRESOURCE, "safe.site.com")!!,
            AdBlockRequest.of("https://cdn.example.com/ads/ok.js", RequestKind.SUBRESOURCE, "shop.fr")!!,
            AdBlockRequest.of("https://tracker.example.org/", RequestKind.SUBRESOURCE, "page.com")!!,
            AdBlockRequest.of("https://ads.example.com/", RequestKind.SUBRESOURCE, "optout.example.org", "optout.example.org")!!,
        )
        assertEquals(
            listOf(true, true, false, true, false, true, false, true, true, false),
            requests.map(read::isBlocked),
        )
        assertEquals(requests.map(original::isBlocked), requests.map(read::isBlocked))
    }

    @Test(expected = IOException::class)
    fun notCompiledRulesAreRefused() {
        DataInputStream(ByteArrayInputStream("not rules at all".toByteArray())).use { AdBlockRules.read(it, ::site) }
    }
}
