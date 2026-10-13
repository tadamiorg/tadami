package com.sf.tadami.extension.util

import com.sf.tadami.extension.model.ExtensionLoadIssue
import com.sf.tadami.extension.model.LibVersion
import com.sf.tadami.utils.Lang
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtensionsLoaderTest {

    @Test
    fun libVersionsOutsideTheSupportedRangeGiveTheirCause() {
        fun issue(versionName: String?) =
            ExtensionsLoader.libVersionIssue(versionName?.let { LibVersion.fromVersionName(it) })

        assertNull(issue("${ExtensionsLoader.API_VERSION_MIN}.4"))
        assertNull(issue("${ExtensionsLoader.API_VERSION_MAX}.12"))
        assertEquals(ExtensionLoadIssue.LIB_TOO_OLD, issue("1.4.12"))
        assertEquals(ExtensionLoadIssue.LIB_TOO_OLD, issue("1.3.5"))
        assertEquals(ExtensionLoadIssue.LIB_TOO_OLD, issue("1.2.30"))
        assertEquals(ExtensionLoadIssue.LIB_TOO_OLD, issue("0.9.1"))
        assertEquals(ExtensionLoadIssue.LIB_TOO_NEW, issue("1.7.1"))
        assertEquals(ExtensionLoadIssue.LIB_TOO_NEW, issue("2.0.1"))
        assertEquals(ExtensionLoadIssue.INVALID_VERSION, issue(null))
        assertEquals(ExtensionLoadIssue.INVALID_VERSION, issue(""))
        assertEquals(ExtensionLoadIssue.INVALID_VERSION, issue("1.5"))
        assertEquals(ExtensionLoadIssue.INVALID_VERSION, issue("beta.1.2"))
    }

    @Test
    fun onlyExtensionsSignedWithAnOfficialOrUserTrustedKeyAreTrusted() {
        val official = "11dec208a209773efdc5348af5de709ca9af067e9154d1dd3c6abdb0fa3cc200"
        val other = "ab".repeat(32)
        val rotated = "cd".repeat(32)

        assertTrue(ExtensionsLoader.isTrusted(listOf(official), emptySet()))
        assertFalse(ExtensionsLoader.isTrusted(listOf(other), emptySet()))
        assertTrue(ExtensionsLoader.isTrusted(listOf(other), setOf(other)))
        // A key rotated from a trusted one: the platform verified its history
        assertTrue(ExtensionsLoader.isTrusted(listOf(other, rotated), setOf(other)))
        assertFalse(ExtensionsLoader.isTrusted(listOf(rotated), setOf(other)))
        assertFalse(ExtensionsLoader.isTrusted(emptyList(), setOf(other)))
    }

    @Test
    fun signatureHashesAreLowercaseHexSha256() {
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            ExtensionsLoader.sha256("abc".toByteArray()),
        )
    }

    @Test
    fun languageComesFromThePackageName() {
        assertEquals(Lang.ENGLISH, ExtensionsLoader.langFromPackageName("com.sf.tadami.extension.en.animepahe"))
        assertEquals(Lang.FRENCH, ExtensionsLoader.langFromPackageName("com.sf.tadami.extension.fr.franime"))
        assertEquals(Lang.UNKNOWN, ExtensionsLoader.langFromPackageName("com.sf.tadami.extension.all.foo"))
        assertEquals(Lang.UNKNOWN, ExtensionsLoader.langFromPackageName("org.example.fr.other"))
    }
}
