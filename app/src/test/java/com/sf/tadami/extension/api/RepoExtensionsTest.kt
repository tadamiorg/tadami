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
    }

    @Test
    fun anExtensionOfARemovedRepoIsKeptButNotObsolete() {
        // The repo "dead" was removed: only the official one is read
        val repo = RepoExtensions.of(mapOf(official to listOf(available("official.ext", repoUrl = official))), emptyList())

        assertFalse(repo.isObsolete("dead.ext", dead)!!)
        // Even when the official repo lists the same package, it isn't updated from there
        val installed = Extension.Installed(
            name = "dead.ext",
            pkgName = "official.ext",
            versionName = "1",
            versionCode = 1,
            apiVersion = ExtensionsLoader.API_VERSION_MAX,
            lang = Lang.ENGLISH,
            pkgFactory = null,
            sources = emptyList(),
            icon = null,
            repoUrl = dead,
        )
        assertNull(installed.listedIn(repo.available))
        assertEquals(official, installed.copy(repoUrl = null).listedIn(repo.available)?.repoUrl)
        assertEquals(official, installed.copy(repoUrl = official).listedIn(repo.available)?.repoUrl)
    }

    private val realOfficial = "https://raw.githubusercontent.com/tadamiorg/tadami-extensions/main"
    private val other = "https://example.com/repo"
    private val third = "https://example.org/repo"

    @Test
    fun aPackageListedBySeveralReposIsShownOncePreferringItsInstalledRepoThenTheOfficialOne() {
        val available = listOf(
            available("shared", repoUrl = other),
            available("shared", repoUrl = realOfficial),
            available("shared", repoUrl = third),
            available("only.other", repoUrl = other),
            available("first.wins", repoUrl = third),
            available("first.wins", repoUrl = other),
        )

        assertEquals(
            listOf("shared" to realOfficial, "only.other" to other, "first.wins" to third),
            RepoExtensions.deduplicated(available).map { it.pkgName to it.repoUrl },
        )
        assertEquals(
            listOf("shared" to third, "only.other" to other, "first.wins" to other),
            RepoExtensions.deduplicated(available, mapOf("shared" to third, "first.wins" to other))
                .map { it.pkgName to it.repoUrl },
        )
    }

    @Test
    fun anInstalledExtensionIsOnlyUpdatedFromItsRepoOrTheOfficialOneWhenItsRepoIsUnknown() {
        val available = listOf(available("shared", repoUrl = other), available("shared", repoUrl = realOfficial))
        val installed = Extension.Installed(
            name = "shared",
            pkgName = "shared",
            versionName = "1",
            versionCode = 1,
            apiVersion = ExtensionsLoader.API_VERSION_MAX,
            lang = Lang.ENGLISH,
            pkgFactory = null,
            sources = emptyList(),
            icon = null,
        )

        assertEquals(other, installed.copy(repoUrl = other).listedIn(available)?.repoUrl)
        assertEquals(realOfficial, installed.listedIn(available)?.repoUrl)
        assertNull(installed.copy(repoUrl = third).listedIn(available))
    }

    @Test
    fun withoutAnyRepoNoExtensionIsObsolete() {
        val repo = RepoExtensions.of(emptyMap(), previous = listOf(available("official.ext", repoUrl = official)))

        assertFalse(repo.hasRepos)
        assertEquals(emptyList<Extension.Available>(), repo.available)
        assertFalse(repo.isObsolete("official.ext", official)!!)
        assertFalse(repo.isObsolete("official.ext", null)!!)
    }

    @Test
    fun anExtensionIsObsoleteWhenItsOwnRepoNoLongerListsIt() {
        val repo = RepoExtensions.of(
            mapOf(official to listOf(available("moved", repoUrl = official)), other to emptyList()),
            emptyList(),
        )

        // Listed by another repo, not by its own
        assertTrue(repo.isObsolete("moved", other)!!)
        assertFalse(repo.isObsolete("moved", official)!!)
        // Unknown repo: listed by one of them
        assertFalse(repo.isObsolete("moved", null)!!)
    }
}
