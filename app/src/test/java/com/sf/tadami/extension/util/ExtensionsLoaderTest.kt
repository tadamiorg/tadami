package com.sf.tadami.extension.util

import com.sf.tadami.extension.model.ExtensionLoadIssue
import com.sf.tadami.extension.model.LibVersion
import com.sf.tadami.utils.Lang
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExtensionsLoaderTest {

    @Test
    fun libVersionsOutsideTheSupportedRangeGiveTheirCause() {
        fun issue(versionName: String?) =
            ExtensionsLoader.libVersionIssue(versionName?.let { LibVersion.fromVersionName(it) })

        assertNull(issue("${ExtensionsLoader.API_VERSION_MIN}.4"))
        assertNull(issue("${ExtensionsLoader.API_VERSION_MAX}.12"))
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
    fun languageComesFromThePackageName() {
        assertEquals(Lang.ENGLISH, ExtensionsLoader.langFromPackageName("com.sf.tadami.extension.en.animepahe"))
        assertEquals(Lang.FRENCH, ExtensionsLoader.langFromPackageName("com.sf.tadami.extension.fr.franime"))
        assertEquals(Lang.UNKNOWN, ExtensionsLoader.langFromPackageName("com.sf.tadami.extension.all.foo"))
        assertEquals(Lang.UNKNOWN, ExtensionsLoader.langFromPackageName("org.example.fr.other"))
    }
}
