package com.sf.tadami.extension.util

import com.sf.tadami.domain.extensions.Extension
import com.sf.tadami.extension.model.ExtensionLoadIssue
import com.sf.tadami.extension.model.LibVersion
import com.sf.tadami.extension.model.SourceIdConflict
import com.sf.tadami.preferences.extensions.SourceIdOwnersPreferences
import com.sf.tadami.source.Source
import com.sf.tadami.utils.Lang
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceIdConflictsTest {

    private class TestSource(override val id: Long, override val name: String = "Source $id") : Source {
        override val lang = Lang.ENGLISH
    }

    private val official = mutableSetOf<String>()
    private val installedAt = mutableMapOf<String, Long>()

    private fun extension(
        pkgName: String,
        vararg ids: Long,
        loadIssue: ExtensionLoadIssue? = null,
    ) = Extension.Installed(
        name = pkgName.substringAfterLast('.').replaceFirstChar { it.uppercase() },
        pkgName = pkgName,
        versionName = "1.7.1",
        versionCode = 1,
        apiVersion = LibVersion(1, 7),
        lang = Lang.ENGLISH,
        pkgFactory = null,
        sources = if (loadIssue == null) ids.map { TestSource(it) } else emptyList(),
        icon = null,
        loadIssue = loadIssue,
    )

    private fun resolve(extensions: List<Extension.Installed>, owners: Map<Long, String> = emptyMap()) =
        resolveSourceIdConflicts(
            extensions = extensions,
            owners = owners,
            isOfficial = { it.pkgName in official },
            installedAt = { installedAt[it] ?: 0L },
        )

    private fun List<Extension.Installed>.byPkg(pkgName: String) = single { it.pkgName == pkgName }

    @Test
    fun extensionsWithoutConflictKeepTheirSourcesAndOwnTheirIds() {
        val (extensions, owners) = resolve(listOf(extension("a.one", 1, 2), extension("a.two", 3)))

        assertTrue(extensions.all { it.loadIssue == null && it.sourceIdConflict == null })
        assertEquals(listOf(1L, 2L), extensions.byPkg("a.one").sources.map { it.id })
        assertEquals(mapOf(1L to "a.one", 2L to "a.one", 3L to "a.two"), owners)
    }

    @Test
    fun anExtensionWithTwoSourcesOfTheSameIdIsRefused() {
        val (extensions, owners) = resolve(listOf(extension("a.dup", 9, 9), extension("a.two", 3)))

        val refused = extensions.byPkg("a.dup")
        assertEquals(ExtensionLoadIssue.SOURCE_ID_CONFLICT, refused.loadIssue)
        assertEquals(SourceIdConflict.Duplicate, refused.sourceIdConflict)
        assertTrue(refused.sources.isEmpty())
        assertEquals(listOf(9L, 9L), refused.refusedSources.map { it.id })
        // It owns nothing
        assertEquals(mapOf(3L to "a.two"), owners)
    }

    @Test
    fun onFirstSightTheOfficialExtensionGetsTheId() {
        official += "official.ext"
        installedAt["third.party"] = 1L
        installedAt["official.ext"] = 2L

        val (extensions, owners) = resolve(listOf(extension("third.party", 50), extension("official.ext", 50, 51)))

        assertNull(extensions.byPkg("official.ext").loadIssue)
        val refused = extensions.byPkg("third.party")
        assertEquals(ExtensionLoadIssue.SOURCE_ID_CONFLICT, refused.loadIssue)
        assertEquals(SourceIdConflict.TakenBy("official.ext", "Ext"), refused.sourceIdConflict)
        assertEquals(mapOf(50L to "official.ext", 51L to "official.ext"), owners)
    }

    @Test
    fun onFirstSightTheExtensionInstalledFirstGetsTheId() {
        installedAt["b.later"] = 20L
        installedAt["c.earlier"] = 10L

        val (extensions, owners) = resolve(listOf(extension("b.later", 7), extension("c.earlier", 7)))

        assertNull(extensions.byPkg("c.earlier").loadIssue)
        assertEquals(SourceIdConflict.TakenBy("c.earlier", "Earlier"), extensions.byPkg("b.later").sourceIdConflict)
        assertEquals(mapOf(7L to "c.earlier"), owners)
    }

    @Test
    fun theOwnerIsKeptAgainstALaterExtensionEvenAnOfficialOne() {
        official += "official.ext"

        val (extensions, owners) = resolve(
            listOf(extension("official.ext", 5), extension("third.party", 5)),
            owners = mapOf(5L to "third.party"),
        )

        assertNull(extensions.byPkg("third.party").loadIssue)
        assertEquals(SourceIdConflict.TakenBy("third.party", "Party"), extensions.byPkg("official.ext").sourceIdConflict)
        assertEquals(mapOf(5L to "third.party"), owners)
    }

    @Test
    fun anOwnerThatCantBeUsedStaysTheOwnerWhileInstalled() {
        val untrusted = extension("a.owner", loadIssue = ExtensionLoadIssue.UNTRUSTED)

        val (extensions, owners) = resolve(listOf(untrusted, extension("a.newcomer", 4)), owners = mapOf(4L to "a.owner"))

        assertEquals(SourceIdConflict.TakenBy("a.owner", "Owner"), extensions.byPkg("a.newcomer").sourceIdConflict)
        assertEquals(ExtensionLoadIssue.UNTRUSTED, extensions.byPkg("a.owner").loadIssue)
        assertEquals(mapOf(4L to "a.owner"), owners)
    }

    @Test
    fun aRefusedExtensionGetsItsSourcesBackOnceItsOwnerIsUninstalled() {
        val (refused, owners) = resolve(listOf(extension("a.owner", 4), extension("a.newcomer", 4, 6)), mapOf(4L to "a.owner"))
        assertEquals(ExtensionLoadIssue.SOURCE_ID_CONFLICT, refused.byPkg("a.newcomer").loadIssue)

        // Unchanged while the owner is installed
        val (again, sameOwners) = resolve(refused, owners)
        assertEquals(refused, again)
        assertEquals(owners, sameOwners)

        val (restored, newOwners) = resolve(again.filter { it.pkgName != "a.owner" }, sameOwners)
        val newcomer = restored.single()
        assertNull(newcomer.loadIssue)
        assertNull(newcomer.sourceIdConflict)
        assertEquals(listOf(4L, 6L), newcomer.sources.map { it.id })
        assertTrue(newcomer.refusedSources.isEmpty())
        assertEquals(mapOf(4L to "a.newcomer", 6L to "a.newcomer"), newOwners)
    }

    @Test
    fun anUpdateKeepsTheIdsOfItsPackage() {
        val (extensions, owners) = resolve(listOf(extension("a.ext", 1, 2, 3)), owners = mapOf(1L to "a.ext", 2L to "a.ext"))

        assertNull(extensions.single().loadIssue)
        assertEquals(mapOf(1L to "a.ext", 2L to "a.ext", 3L to "a.ext"), owners)
    }

    @Test
    fun theInstallOfAnExtensionTellsItsConflict() {
        val installed = listOf(extension("a.owner", 4), extension("a.other", 8))
        val owners = mapOf(4L to "a.owner", 8L to "a.other")
        fun conflictOf(pkgName: String, vararg ids: Long, official: Boolean = false) =
            sourceIdConflictOf(pkgName, ids.toList(), official, installed, owners) { false }

        assertEquals(SourceIdConflict.TakenBy("a.owner", "Owner"), conflictOf("a.newcomer", 10, 4))
        // An official extension doesn't take an owned ID either
        assertEquals(SourceIdConflict.TakenBy("a.other", "Other"), conflictOf("a.newcomer", 8, official = true))
        assertEquals(SourceIdConflict.Duplicate, conflictOf("a.newcomer", 11, 11))
        assertNull(conflictOf("a.newcomer", 10, 11))
        // The update of an installed one, which keeps its IDs
        assertNull(conflictOf("a.owner", 4, 5))
    }

    @Test
    fun ownersAreStoredAsIdPackagePairs() {
        val owners = mapOf(1L to "com.sf.tadami.extension.en.gogoanime", Long.MAX_VALUE to "x.y")
        assertEquals(owners, SourceIdOwnersPreferences.decode(SourceIdOwnersPreferences.encode(owners)))
        assertEquals(mapOf(3L to "a.b"), SourceIdOwnersPreferences.decode(setOf("3=a.b", "x=a.b", "4=", "5")))
        assertEquals(emptyMap<Long, String>(), SourceIdOwnersPreferences.decode(null))
    }
}
