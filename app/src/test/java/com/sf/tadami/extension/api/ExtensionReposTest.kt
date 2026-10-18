package com.sf.tadami.extension.api

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtensionReposTest {

    private fun candidates(input: String, allowHttp: Boolean = false) =
        (ExtensionRepos.parse(input, allowHttp) as ExtensionRepos.Input.Candidates).urls

    @Test
    fun aRepoUrlLosesItsIndexFileAndTrailingSlashes() {
        assertEquals(listOf("https://example.com/repo"), candidates("https://example.com/repo"))
        assertEquals(listOf("https://example.com/repo"), candidates(" https://example.com/repo/ "))
        assertEquals(listOf("https://example.com/repo"), candidates("https://example.com/repo/index.min.json"))
        assertEquals(listOf("https://example.com/repo"), candidates("https://example.com/repo/index.min.json/"))
    }

    @Test
    fun aGitHubRepoIsTriedOnItsUsualBranches() {
        assertEquals(
            listOf(
                "https://raw.githubusercontent.com/owner/exts/main",
                "https://raw.githubusercontent.com/owner/exts/master",
                "https://raw.githubusercontent.com/owner/exts/repo",
            ),
            candidates("https://github.com/owner/exts/"),
        )
        // Deeper than a repo, it's taken as it is
        assertEquals(listOf("https://github.com/owner/exts/tree/main"), candidates("https://github.com/owner/exts/tree/main"))
    }

    @Test
    fun httpIsRefusedButInDebugBuilds() {
        assertEquals(ExtensionRepos.Input.NotHttps, ExtensionRepos.parse("http://10.0.2.2:8000", allowHttp = false))
        assertEquals(listOf("http://10.0.2.2:8000"), candidates("http://10.0.2.2:8000/", allowHttp = true))
    }

    @Test
    fun whatIsNoUrlIsRefused() {
        assertEquals(ExtensionRepos.Input.Invalid, ExtensionRepos.parse("", allowHttp = true))
        assertEquals(ExtensionRepos.Input.Invalid, ExtensionRepos.parse("example.com/repo", allowHttp = true))
        assertEquals(ExtensionRepos.Input.Invalid, ExtensionRepos.parse("ftp://example.com/repo", allowHttp = true))
        assertEquals(ExtensionRepos.Input.Invalid, ExtensionRepos.parse("https://", allowHttp = true))
        assertEquals(ExtensionRepos.Input.Invalid, ExtensionRepos.parse("https://exa mple.com", allowHttp = true))
    }

    @Test
    fun aGitHubRepoIsNamedOwnerSlashRepo() {
        assertEquals("tadamiorg/tadami-extensions", ExtensionRepos.name("https://raw.githubusercontent.com/tadamiorg/tadami-extensions/main"))
        assertEquals("someone/exts", ExtensionRepos.name("https://raw.githubusercontent.com/someone/exts/repo/sub/"))
        assertEquals("someone/exts", ExtensionRepos.name("https://github.com/someone/exts"))
        assertEquals("someone/exts", ExtensionRepos.name("https://www.github.com/someone/exts/"))
    }

    @Test
    fun anotherRepoIsNamedByItsHostAndPath() {
        assertEquals("example.com/tadami/repo", ExtensionRepos.name("https://example.com/tadami/repo/"))
        assertEquals("example.com/tadami/repo", ExtensionRepos.name("https://example.com/tadami/repo/index.min.json"))
        assertEquals("example.com", ExtensionRepos.name("http://example.com//"))
    }

    @Test
    fun theOfficialRepoIsRecognized() {
        assertTrue(ExtensionRepos.isOfficial("https://raw.githubusercontent.com/tadamiorg/tadami-extensions/main"))
        assertTrue(ExtensionRepos.isOfficial("https://raw.githubusercontent.com/tadamiorg/tadami-extensions/main/"))
        assertFalse(ExtensionRepos.isOfficial("https://raw.githubusercontent.com/someone/tadami-extensions/main"))
        assertFalse(ExtensionRepos.isOfficial("https://raw.githubusercontent.com/tadamiorg/tadami-extensions/dev"))
    }

    @Test
    fun noLookalikeOfTheOfficialRepoIsOfficial() {
        listOf(
            "http://raw.githubusercontent.com/tadamiorg/tadami-extensions/main",
            "https://raw.githubusercontent.com/tadami0rg/tadami-extensions/main",
            "https://raw.githubusercontent.com/tadamiorg/tadami-extensions/main/repo",
            "https://raw.githubusercontent.com.evil.com/tadamiorg/tadami-extensions/main",
            "https://github.com/tadamiorg/tadami-extensions",
            "https://evil.com/raw.githubusercontent.com/tadamiorg/tadami-extensions/main",
        ).forEach { assertFalse(it, ExtensionRepos.isOfficial(it)) }
    }

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun aRepoJsonGivesItsNameAndKeyFingerprint() {
        val meta = ExtensionRepos.parseMeta(
            json,
            """{"meta":{"name":"Test repo","shortName":"Test","website":"https://example.com",
                "signingKeyFingerprint":"11:DE:C2:08:A2:09:77:3E:FD:C5:34:8A:F5:DE:70:9C:A9:AF:06:7E:91:54:D1:DD:3C:6A:BD:B0:FA:3C:C2:00"},
                "other":1}""",
        )!!
        assertEquals("Test repo", meta.name)
        assertEquals("Test", meta.shortName)
        assertEquals("https://example.com", meta.website)
        assertEquals("11dec208a209773efdc5348af5de709ca9af067e9154d1dd3c6abdb0fa3cc200", meta.signingKeyFingerprint)
    }

    @Test
    fun aFingerprintThatIsNoSha256IsDropped() {
        val meta = ExtensionRepos.parseMeta(json, """{"meta":{"name":"Test","signingKeyFingerprint":"abcd"}}""")!!
        assertNull(meta.signingKeyFingerprint)
    }

    @Test
    fun aRepoJsonWithoutNameIsNoDescription() {
        assertNull(ExtensionRepos.parseMeta(json, """{"meta":{"name":" "}}"""))
        assertNull(ExtensionRepos.parseMeta(json, """{"name":"Test"}"""))
        assertNull(ExtensionRepos.parseMeta(json, "<html>"))
    }
}
