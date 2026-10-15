package com.sf.tadami.extension.api

import com.sf.tadami.domain.extensions.Extension
import com.sf.tadami.extension.model.LibVersion
import com.sf.tadami.extension.util.ExtensionsLoader
import com.sf.tadami.utils.Lang
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RepoExtensionsTest {

    private fun available(pkgName: String, libVersion: LibVersion = ExtensionsLoader.API_VERSION_MAX, repoUrl: String = "") = Extension.Available(
        name = pkgName,
        pkgName = pkgName,
        versionName = "$libVersion.1",
        versionCode = 1,
        apiVersion = libVersion,
        lang = Lang.ENGLISH,
        sources = emptyList(),
        apkName = "$pkgName.apk",
        iconUrl = "",
        repoUrl = repoUrl,
    )

    @Test
    fun extensionsMadeForAnotherLibAreListedButNotAvailable() {
        val repo = RepoExtensions(
            listOf(
                available("supported", ExtensionsLoader.API_VERSION_MAX),
                available("too.new", LibVersion(ExtensionsLoader.API_VERSION_MAX.major, ExtensionsLoader.API_VERSION_MAX.minor + 1)),
                available("too.old", LibVersion(1, 2)),
            ),
        )

        assertEquals(listOf("supported"), repo.available.map { it.pkgName })
        assertEquals(setOf("supported", "too.new", "too.old"), repo.listedPkgNames)
    }

    private val official = "https://official"
    private val dead = "http://127.0.0.1:8765"

    @Test
    fun aRepoThatFailsKeepsItsPreviousEntriesWithoutHidingTheOthers() {
        val previous = listOf(available("official.old", repoUrl = official), available("dead.ext", repoUrl = dead))

        val repo = RepoExtensions.of(
            mapOf(official to listOf(available("official.new", repoUrl = official)), dead to null),
            previous,
        )

        assertEquals(listOf("official.new", "dead.ext"), repo.available.map { it.pkgName })
    }

    @Test
    fun aRepoThatFailsOnAColdStartGivesNoEntries() {
        val repo = RepoExtensions.of(
            mapOf(official to listOf(available("official.ext", repoUrl = official)), dead to null),
            previous = emptyList(),
        )

        assertEquals(listOf("official.ext"), repo.available.map { it.pkgName })
    }

    @Test
    fun anExtensionThatMayComeFromARepoThatFailedIsNotObsolete() {
        val failing = RepoExtensions.of(
            mapOf(official to listOf(available("official.ext", repoUrl = official)), dead to null),
            previous = emptyList(),
        )
        assertFalse(failing.isObsolete("official.ext", official)!!)
        // Unlisted by the repo it comes from, which was read
        assertTrue(failing.isObsolete("official.removed", official)!!)
        // From the repo that failed, or from an unknown one
        assertNull(failing.isObsolete("dead.ext", dead))
        assertNull(failing.isObsolete("dead.ext", null))

        val allRead = RepoExtensions.of(mapOf(official to listOf(available("official.ext", repoUrl = official))), emptyList())
        assertTrue(allRead.isObsolete("official.removed", null)!!)
        assertTrue(allRead.isObsolete("dead.ext", dead)!!)
    }
}
