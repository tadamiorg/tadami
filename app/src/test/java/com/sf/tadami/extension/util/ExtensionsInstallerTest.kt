package com.sf.tadami.extension.util

import com.sf.tadami.domain.extensions.Extension
import com.sf.tadami.extension.model.LibVersion
import com.sf.tadami.utils.Lang
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ExtensionsInstallerTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val pkgName = "com.sf.tadami.extension.en.animepahe"
    private val key = "ab".repeat(32)
    private val otherKey = "cd".repeat(32)
    private val rotatedKey = "ef".repeat(32)

    private fun apk(
        versionCode: Long = 2,
        versionName: String = "1.5.$versionCode",
        signatureHashes: List<String> = listOf(key),
        pkgName: String = this.pkgName,
        isExtension: Boolean = true,
        hasNativeLibs: Boolean = false,
        multipleSigners: Boolean = false,
    ) = ApkInfo(pkgName, versionCode, versionName, isExtension, hasNativeLibs, signatureHashes, multipleSigners)

    private fun listed(versionCode: Long = 2, versionName: String = "1.5.$versionCode") = Extension.Available(
        name = "AnimePahe",
        pkgName = pkgName,
        versionName = versionName,
        versionCode = versionCode,
        apiVersion = LibVersion(1, 5),
        lang = Lang.ENGLISH,
        sources = emptyList(),
        apkName = "$pkgName.apk",
        iconUrl = "",
        repoUrl = "",
    )

    private fun rejection(
        apk: ApkInfo,
        listed: Extension.Available? = listed(),
        installed: ApkInfo? = null,
        allowSameVersion: Boolean = false,
    ) = ExtensionsInstaller.rejectionOf(apk, pkgName, listed, installed, allowSameVersion)

    @Test
    fun aNewExtensionMustBeTheListedOne() {
        assertNull(rejection(apk()))
        // Its key is checked when it's loaded, an untrusted one isn't loaded
        assertNull(rejection(apk(signatureHashes = listOf(otherKey))))
        assertNull(rejection(apk(versionCode = 7), listed = null))

        assertEquals(ApkRejection.WRONG_PACKAGE, rejection(apk(pkgName = "$pkgName.other")))
        assertEquals(
            ApkRejection.WRONG_PACKAGE,
            ExtensionsInstaller.rejectionOf(apk(pkgName = "../evil"), "../evil", null, null, false),
        )
        assertEquals(ApkRejection.NOT_AN_EXTENSION, rejection(apk(isExtension = false)))
        assertEquals(ApkRejection.WRONG_VERSION, rejection(apk(versionCode = 1)))
        assertEquals(ApkRejection.WRONG_VERSION, rejection(apk(versionName = "1.5.3")))
        assertEquals(ApkRejection.UNSUPPORTED_LIB, rejection(apk(versionName = "1.4.2"), listed = listed(versionName = "1.4.2")))
        assertEquals(ApkRejection.UNSUPPORTED_LIB, rejection(apk(versionName = "2"), listed = null))
        assertEquals(ApkRejection.NATIVE_LIBS, rejection(apk(hasNativeLibs = true)))
    }

    @Test
    fun anUpdateMustBeNewerAndSignedWithTheSameKey() {
        val installed = apk(versionCode = 1)

        assertNull(rejection(apk(), installed = installed))
        assertNull(rejection(apk(signatureHashes = listOf(key, rotatedKey)), installed = installed))

        assertEquals(ApkRejection.SIGNER_CHANGED, rejection(apk(signatureHashes = listOf(otherKey)), installed = installed))
        assertEquals(ApkRejection.SIGNER_CHANGED, rejection(apk(signatureHashes = emptyList()), installed = installed))
        assertEquals(ApkRejection.NOT_NEWER, rejection(apk(versionCode = 1), listed(versionCode = 1), installed))
        assertEquals(ApkRejection.NOT_NEWER, rejection(apk(versionCode = 1), listed(versionCode = 1), apk(versionCode = 2)))
        // Allowed when the installed one can't be used
        assertNull(rejection(apk(versionCode = 1), listed(versionCode = 1), installed, allowSameVersion = true))
        assertEquals(
            ApkRejection.NOT_NEWER,
            rejection(apk(versionCode = 1), listed(versionCode = 1), apk(versionCode = 2), allowSameVersion = true),
        )
    }

    @Test
    fun anUpdateCanOnlyRaiseTheLibVersion() {
        val installed = apk(versionName = "1.5.2")

        assertNull(rejection(apk(versionName = "1.6.2"), listed(versionName = "1.6.2"), installed))
        // One whose version name can't be parsed can't be used: replaced by a valid one
        assertNull(rejection(apk(versionName = "1.6.2"), listed(versionName = "1.6.2"), apk(versionName = "2")))

        assertEquals(ApkRejection.NOT_NEWER, rejection(apk(), listed(), installed))
        assertEquals(ApkRejection.NOT_NEWER, rejection(apk(), listed(), apk(versionName = "1.6.2")))
        // A higher lib doesn't make up for a lower version code
        assertEquals(
            ApkRejection.NOT_NEWER,
            rejection(apk(versionCode = 1, versionName = "1.6.1"), listed(versionCode = 1, versionName = "1.6.1"), installed),
        )
    }

    @Test
    fun signersFollowTheAndroidUpdateRule() {
        fun canReplace(old: ApkInfo, new: ApkInfo) = ExtensionsInstaller.canReplaceSigners(old, new)

        assertTrue(canReplace(apk(signatureHashes = listOf(key)), apk(signatureHashes = listOf(key))))
        // Rotated from the installed key
        assertTrue(canReplace(apk(signatureHashes = listOf(key)), apk(signatureHashes = listOf(key, rotatedKey))))
        assertTrue(canReplace(apk(signatureHashes = listOf(key, rotatedKey)), apk(signatureHashes = listOf(key, rotatedKey))))
        // Back to the key it was rotated from
        assertFalse(canReplace(apk(signatureHashes = listOf(key, rotatedKey)), apk(signatureHashes = listOf(key))))
        assertFalse(canReplace(apk(signatureHashes = listOf(key)), apk(signatureHashes = listOf(otherKey))))
        assertFalse(canReplace(apk(signatureHashes = listOf(key)), apk(signatureHashes = emptyList())))
        assertFalse(canReplace(apk(signatureHashes = emptyList()), apk(signatureHashes = emptyList())))

        val signers = apk(signatureHashes = listOf(key, otherKey), multipleSigners = true)
        assertTrue(canReplace(signers, apk(signatureHashes = listOf(otherKey, key), multipleSigners = true)))
        assertFalse(canReplace(signers, apk(signatureHashes = listOf(key), multipleSigners = true)))
        assertFalse(canReplace(signers, apk(signatureHashes = listOf(key, otherKey))))
        assertFalse(canReplace(apk(signatureHashes = listOf(key)), apk(signatureHashes = listOf(key), multipleSigners = true)))
    }

    @Test
    fun downloadsStopPastTheSizeLimit() {
        val file = tmpFolder.newFile()
        val bytes = ByteArray(10_000) { it.toByte() }

        val sha256 = ExtensionsInstaller.copyAtMost(bytes.inputStream(), file, maxBytes = 10_000)
        assertArrayEquals(bytes, file.readBytes())
        assertEquals(ExtensionsLoader.sha256(bytes), sha256)

        assertThrows(IOException::class.java) {
            ExtensionsInstaller.copyAtMost(bytes.inputStream(), file, maxBytes = 9_999)
        }
    }

    @Test
    fun aDownloadMustBeTheSizeAndHashTheRepoLists() {
        fun rejection(listedSize: Long?, listedSha256: String?, length: Long, sha256: String?) =
            ExtensionsInstaller.listedFileRejection(listedSize, listedSha256, length, sha256)
        val sha256 = "ab".repeat(32)

        // Older repos list neither
        assertNull(rejection(null, null, length = 1_000, sha256 = sha256))
        assertNull(rejection(null, null, length = -1, sha256 = null))

        // Before the download: its Content-Length, unknown for a chunked response
        assertNull(rejection(1_000, sha256, length = 1_000, sha256 = null))
        assertNull(rejection(1_000, sha256, length = -1, sha256 = null))
        assertEquals(ApkRejection.NOT_LISTED_FILE, rejection(1_000, sha256, length = 999, sha256 = null))
        assertEquals(ApkRejection.NOT_LISTED_FILE, rejection(1_000, null, length = 1_001, sha256 = null))

        // Once downloaded
        assertNull(rejection(1_000, sha256, length = 1_000, sha256 = sha256))
        assertNull(rejection(1_000, sha256.uppercase(), length = 1_000, sha256 = sha256))
        assertNull(rejection(null, sha256, length = 1_000, sha256 = sha256))
        assertEquals(ApkRejection.NOT_LISTED_FILE, rejection(1_000, sha256, length = 1_000, sha256 = "cd".repeat(32)))
        assertEquals(ApkRejection.NOT_LISTED_FILE, rejection(null, sha256, length = 1_000, sha256 = "cd".repeat(32)))
        assertEquals(ApkRejection.NOT_LISTED_FILE, rejection(1_000, sha256, length = 500, sha256 = sha256))
    }

    @Test
    fun nativeLibrariesAreFound() {
        fun zip(vararg entries: String): File = tmpFolder.newFile().apply {
            ZipOutputStream(outputStream()).use { zip ->
                entries.forEach {
                    zip.putNextEntry(ZipEntry(it))
                    zip.closeEntry()
                }
            }
        }

        assertFalse(ExtensionsInstaller.hasNativeLibs(zip("AndroidManifest.xml", "classes.dex", "res/lib/a.xml")))
        assertTrue(ExtensionsInstaller.hasNativeLibs(zip("classes.dex", "lib/arm64-v8a/libfoo.so")))
    }
}
