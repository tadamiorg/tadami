package com.sf.tadami.ui.tabs.browse.tabs.extensions.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtensionDialogsTest {

    @Test
    fun theFingerprintIsUppercaseInGroupsOfFour() {
        assertEquals(
            "11DE C208 A209 773E FDC5 348A F5DE 709C A9AF 067E 9154 D1DD 3C6A BDB0 FA3C C200",
            groupedFingerprint("11dec208a209773efdc5348af5de709ca9af067e9154d1dd3c6abdb0fa3cc200"),
        )
    }

    @Test
    fun aGitHubRawRepoIsNamedOwnerSlashRepo() {
        assertEquals("tadamiorg/tadami-extensions", repoName("https://raw.githubusercontent.com/tadamiorg/tadami-extensions/main"))
        assertEquals("someone/exts", repoName("https://raw.githubusercontent.com/someone/exts/repo/sub/"))
    }

    @Test
    fun aGitHubRepoIsNamedOwnerSlashRepo() {
        assertEquals("someone/exts", repoName("https://github.com/someone/exts"))
        assertEquals("someone/exts", repoName("https://www.github.com/someone/exts/"))
    }

    @Test
    fun anotherRepoIsNamedByItsHostAndPath() {
        assertEquals("example.com/tadami/repo", repoName("https://example.com/tadami/repo/"))
        assertEquals("example.com/tadami/repo", repoName("https://example.com/tadami/repo/index.min.json"))
        assertEquals("example.com", repoName("http://example.com//"))
    }

    @Test
    fun theOfficialRepoIsRecognized() {
        assertTrue(isOfficialRepo("https://raw.githubusercontent.com/tadamiorg/tadami-extensions/main"))
        assertTrue(isOfficialRepo("https://raw.githubusercontent.com/tadamiorg/tadami-extensions/main/"))
        assertFalse(isOfficialRepo("https://raw.githubusercontent.com/someone/tadami-extensions/main"))
        assertFalse(isOfficialRepo("https://raw.githubusercontent.com/tadamiorg/tadami-extensions/dev"))
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
        ).forEach { assertFalse(it, isOfficialRepo(it)) }
    }
}
