package com.sf.tadami.extension.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ExtensionsStorageTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val pkgName = "com.sf.tadami.extension.en.animepahe"

    @Test
    fun onlyPackageNamesThatStayInTheFolderAreValid() {
        listOf(pkgName, "a.b", "A_1.b_2").forEach { assertTrue(it, ExtensionsStorage.isValidPkgName(it)) }
        listOf("", "com", "../com.evil", "com/../../evil", "com..evil", ".com.evil", "com.evil.", "com.1evil",
            "com.evil-123", "com.evil/x", "com.evil\\x").forEach { assertFalse(it, ExtensionsStorage.isValidPkgName(it)) }

        val dir = tmpFolder.root
        assertThrows(IllegalArgumentException::class.java) { ExtensionsStorage.apkFile(dir, "../com.evil") }
        assertThrows(IllegalArgumentException::class.java) { ExtensionsStorage.newTmpFile(dir, "../com.evil") }
    }

    @Test
    fun commitMovesTheTmpFileInPlaceReadOnly() {
        val dir = tmpFolder.root
        val tmp = ExtensionsStorage.newTmpFile(dir, pkgName).apply { writeText("v1") }

        val apk = ExtensionsStorage.commit(dir, pkgName, tmp)

        assertEquals(ExtensionsStorage.apkFile(dir, pkgName), apk)
        assertEquals("v1", apk.readText())
        assertFalse(apk.canWrite())
        assertFalse(tmp.exists())
        assertEquals(listOf(pkgName), ExtensionsStorage.pkgNames(dir))
    }

    @Test
    fun commitReplacesTheReadOnlyApkWithoutDeletingItFirst() {
        // Windows doesn't rename over an existing file, Android does (rename(2))
        assumeFalse(System.getProperty("os.name").orEmpty().startsWith("Windows"))
        val dir = tmpFolder.root
        ExtensionsStorage.commit(dir, pkgName, ExtensionsStorage.newTmpFile(dir, pkgName).apply { writeText("v1") })

        val apk = ExtensionsStorage.commit(dir, pkgName, ExtensionsStorage.newTmpFile(dir, pkgName).apply { writeText("v2") })

        assertEquals("v2", apk.readText())
        assertFalse(apk.canWrite())
    }

    @Test
    fun eachDownloadHasItsOwnTmpFileCleanedAtStartup() {
        val dir = File(tmpFolder.root, "extensions")
        val first = ExtensionsStorage.newTmpFile(dir, pkgName)
        val second = ExtensionsStorage.newTmpFile(dir, pkgName)
        File(dir, "$pkgName.apk").writeText("apk")

        assertNotEquals(first, second)
        assertEquals(listOf(pkgName), ExtensionsStorage.pkgNames(dir))

        ExtensionsStorage.cleanTmp(dir)

        assertFalse(first.exists())
        assertFalse(second.exists())
        assertTrue(File(dir, "$pkgName.apk").exists())
    }

    @Test
    fun deleteRemovesTheApkAndWhatArtCompiledOfIt() {
        val dir = tmpFolder.root
        val other = "$pkgName.other"
        val isaDir = File(dir, "oat/arm64").apply { mkdirs() }
        val files = listOf(
            "$pkgName.apk",
            "oat/arm64/$pkgName.odex",
            "oat/arm64/$pkgName.vdex",
            "oat/$pkgName.apk.cur.prof",
        ).map { File(dir, it).apply { writeText("") } }
        val kept = listOf(File(dir, "$other.apk"), File(isaDir, "$other.odex"), File(dir, "oat/$other.apk.cur.prof"))
            .onEach { it.writeText("") }

        assertTrue(ExtensionsStorage.delete(dir, pkgName))

        files.forEach { assertFalse(it.path, it.exists()) }
        kept.forEach { assertTrue(it.path, it.exists()) }
        assertFalse(ExtensionsStorage.delete(dir, pkgName))
    }

    @Test
    fun theStartupDeletesWhatArtLeftOfTheExtensionsWithoutApk() {
        val dir = tmpFolder.root
        val other = "$pkgName.other"
        File(dir, "oat/arm64").mkdirs()
        File(dir, "oat/x86_64").mkdirs()
        val orphans = listOf(
            "oat/$pkgName.apk.cur.prof",
            "oat/$pkgName.apk.prof",
            "oat/arm64/$pkgName.odex",
            "oat/x86_64/$pkgName.vdex",
        ).map { File(dir, it).apply { writeText("") } }
        val kept = listOf(
            "$other.apk",
            "oat/$other.apk.cur.prof",
            "oat/arm64/$other.odex",
            // Not of an extension
            "oat/arm64/unknown",
        ).map { File(dir, it).apply { writeText("") } }

        ExtensionsStorage.cleanOrphanOatFiles(dir)

        orphans.forEach { assertFalse(it.path, it.exists()) }
        kept.forEach { assertTrue(it.path, it.exists()) }
        // Nothing compiled yet
        ExtensionsStorage.cleanOrphanOatFiles(File(tmpFolder.root, "none"))
    }
}
