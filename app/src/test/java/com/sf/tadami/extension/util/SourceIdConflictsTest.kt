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
        val (extensions, owners) = resolve(listOf(extension("a.one", 1001, 1002), extension("a.two", 1003)))

        assertTrue(extensions.all { it.loadIssue == null && it.sourceIdConflict == null })
        assertEquals(listOf(1001L, 1002L), extensions.byPkg("a.one").sources.map { it.id })
        assertEquals(mapOf(1001L to "a.one", 1002L to "a.one", 1003L to "a.two"), owners)
    }

    @Test
    fun anExtensionWithTwoSourcesOfTheSameIdIsRefused() {
        val (extensions, owners) = resolve(listOf(extension("a.dup", 1009, 1009), extension("a.two", 1003)))

        val refused = extensions.byPkg("a.dup")
        assertEquals(ExtensionLoadIssue.SOURCE_ID_CONFLICT, refused.loadIssue)
        assertEquals(SourceIdConflict.Duplicate, refused.sourceIdConflict)
        assertTrue(refused.sources.isEmpty())
        assertEquals(listOf(1009L, 1009L), refused.refusedSources.map { it.id })
        // It owns nothing
        assertEquals(mapOf(1003L to "a.two"), owners)
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

        val (extensions, owners) = resolve(listOf(extension("b.later", 1007), extension("c.earlier", 1007)))

        assertNull(extensions.byPkg("c.earlier").loadIssue)
        assertEquals(SourceIdConflict.TakenBy("c.earlier", "Earlier"), extensions.byPkg("b.later").sourceIdConflict)
        assertEquals(mapOf(1007L to "c.earlier"), owners)
    }

    @Test
    fun theOwnerIsKeptAgainstALaterExtensionEvenAnOfficialOne() {
        official += "official.ext"

        val (extensions, owners) = resolve(
            listOf(extension("official.ext", 1005), extension("third.party", 1005)),
            owners = mapOf(1005L to "third.party"),
        )

        assertNull(extensions.byPkg("third.party").loadIssue)
        assertEquals(SourceIdConflict.TakenBy("third.party", "Party"), extensions.byPkg("official.ext").sourceIdConflict)
        assertEquals(mapOf(1005L to "third.party"), owners)
    }

    @Test
    fun anOwnerThatCantBeUsedStaysTheOwnerWhileInstalled() {
        val untrusted = extension("a.owner", loadIssue = ExtensionLoadIssue.UNTRUSTED)

        val (extensions, owners) = resolve(listOf(untrusted, extension("a.newcomer", 1004)), owners = mapOf(1004L to "a.owner"))

        assertEquals(SourceIdConflict.TakenBy("a.owner", "Owner"), extensions.byPkg("a.newcomer").sourceIdConflict)
        assertEquals(ExtensionLoadIssue.UNTRUSTED, extensions.byPkg("a.owner").loadIssue)
        assertEquals(mapOf(1004L to "a.owner"), owners)
    }

    @Test
    fun aRefusedExtensionGetsItsSourcesBackOnceItsOwnerIsUninstalled() {
        val (refused, owners) = resolve(listOf(extension("a.owner", 1004), extension("a.newcomer", 1004, 1006)), mapOf(1004L to "a.owner"))
        assertEquals(ExtensionLoadIssue.SOURCE_ID_CONFLICT, refused.byPkg("a.newcomer").loadIssue)

        // Unchanged while the owner is installed
        val (again, sameOwners) = resolve(refused, owners)
        assertEquals(refused, again)
        assertEquals(owners, sameOwners)

        val (restored, newOwners) = resolve(again.filter { it.pkgName != "a.owner" }, sameOwners)
        val newcomer = restored.single()
        assertNull(newcomer.loadIssue)
        assertNull(newcomer.sourceIdConflict)
        assertEquals(listOf(1004L, 1006L), newcomer.sources.map { it.id })
        assertTrue(newcomer.refusedSources.isEmpty())
        assertEquals(mapOf(1004L to "a.newcomer", 1006L to "a.newcomer"), newOwners)
    }

    @Test
    fun anUpdateKeepsTheIdsOfItsPackage() {
        val (extensions, owners) = resolve(listOf(extension("a.ext", 1001, 1002, 1003)), owners = mapOf(1001L to "a.ext", 1002L to "a.ext"))

        assertNull(extensions.single().loadIssue)
        assertEquals(mapOf(1001L to "a.ext", 1002L to "a.ext", 1003L to "a.ext"), owners)
    }

    @Test
    fun theInstallOfAnExtensionTellsItsConflict() {
        val installed = listOf(extension("a.owner", 1004), extension("a.other", 1008))
        val owners = mapOf(1004L to "a.owner", 1008L to "a.other")
        fun conflictOf(pkgName: String, vararg ids: Long, official: Boolean = false) =
            sourceIdConflictOf(pkgName, ids.toList(), official, installed, owners) { false }

        assertEquals(SourceIdConflict.TakenBy("a.owner", "Owner"), conflictOf("a.newcomer", 1010, 1004))
        // An official extension doesn't take an owned ID either
        assertEquals(SourceIdConflict.TakenBy("a.other", "Other"), conflictOf("a.newcomer", 1008, official = true))
        assertEquals(SourceIdConflict.Duplicate, conflictOf("a.newcomer", 1011, 1011))
        assertNull(conflictOf("a.newcomer", 1010, 1011))
        // The update of an installed one, which keeps its IDs
        assertNull(conflictOf("a.owner", 1004, 1005))
    }

    @Test
    fun anExtensionNotSignedByTadamiUsingAnOfficialIdIsRefused() {
        official += "com.sf.tadami.extension.fr.animesama"

        val (extensions, owners) = resolve(
            listOf(extension("com.sf.tadami.extension.fr.animesama", 2), extension("fake.animesama", 2), extension("fake.pahe", 7, 2000)),
        )

        assertNull(extensions.byPkg("com.sf.tadami.extension.fr.animesama").loadIssue)
        assertEquals(SourceIdConflict.Reserved, extensions.byPkg("fake.animesama").sourceIdConflict)
        // Even with no official extension of that ID installed
        assertEquals(SourceIdConflict.Reserved, extensions.byPkg("fake.pahe").sourceIdConflict)
        assertEquals(mapOf(2L to "com.sf.tadami.extension.fr.animesama"), owners)
    }

    @Test
    fun theOfficialIdsAreReservedEvenToTheirOwner() {
        // Owned before the official IDs were reserved, re-signed since with another key
        val (extensions, owners) = resolve(listOf(extension("a.resigned", 101)), owners = mapOf(101L to "a.resigned"))

        assertEquals(SourceIdConflict.Reserved, extensions.single().sourceIdConflict)
        // Still its owner while installed: an official one can't take its anime either
        assertEquals(mapOf(101L to "a.resigned"), owners)
    }

    @Test
    fun theInstallOfAnExtensionUsingAnOfficialIdTellsItsConflict() {
        fun conflictOf(vararg ids: Long, official: Boolean) =
            sourceIdConflictOf("a.newcomer", ids.toList(), official, emptyList(), emptyMap()) { false }

        assertEquals(SourceIdConflict.Reserved, conflictOf(15, official = false))
        assertNull(conflictOf(15, official = true))
        assertNull(conflictOf(16, official = false))
    }

    @Test
    fun theOfficialIdsAreThoseOfTheOfficialSources() {
        assertEquals(setOf(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 11L, 15L, 101L, 102L), OFFICIAL_SOURCE_IDS)
    }

    @Test
    fun ownersAreStoredAsIdPackagePairs() {
        val owners = mapOf(1L to "com.sf.tadami.extension.en.gogoanime", Long.MAX_VALUE to "x.y")
        assertEquals(owners, SourceIdOwnersPreferences.decode(SourceIdOwnersPreferences.encode(owners)))
        assertEquals(mapOf(3L to "a.b"), SourceIdOwnersPreferences.decode(setOf("3=a.b", "x=a.b", "4=", "5")))
        assertEquals(emptyMap<Long, String>(), SourceIdOwnersPreferences.decode(null))
    }
}
