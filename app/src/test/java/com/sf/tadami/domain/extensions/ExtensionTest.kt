package com.sf.tadami.domain.extensions

import com.sf.tadami.extension.model.LibVersion
import com.sf.tadami.utils.Lang
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtensionTest {

    private val lib15 = LibVersion(1, 5)
    private val lib16 = LibVersion(1, 6)

    private fun installed(versionCode: Long, libVersion: LibVersion) = Extension.Installed(
        name = "AnimePahe",
        pkgName = "com.sf.tadami.extension.en.animepahe",
        versionName = "$libVersion.$versionCode",
        versionCode = versionCode,
        apiVersion = libVersion,
        lang = Lang.ENGLISH,
        pkgFactory = null,
        sources = emptyList(),
        icon = null,
    )

    private fun available(versionCode: Long, libVersion: LibVersion) = Extension.Available(
        name = "AnimePahe",
        pkgName = "com.sf.tadami.extension.en.animepahe",
        versionName = "$libVersion.$versionCode",
        versionCode = versionCode,
        apiVersion = libVersion,
        lang = Lang.ENGLISH,
        sources = emptyList(),
        apkName = "animepahe.apk",
        iconUrl = "",
        repoUrl = "",
    )

    @Test
    fun anUpdateHasAGreaterVersionCodeOrTheSameOneWithAHigherLib() {
        val installed = installed(versionCode = 2, libVersion = lib15)

        assertTrue(installed.isUpdatedBy(available(versionCode = 3, libVersion = lib15)))
        // Whatever its lib
        assertTrue(installed.isUpdatedBy(available(versionCode = 3, libVersion = LibVersion(1, 4))))
        assertTrue(installed.isUpdatedBy(available(versionCode = 2, libVersion = lib16)))

        assertFalse(installed.isUpdatedBy(available(versionCode = 2, libVersion = lib15)))
        assertFalse(installed.isUpdatedBy(available(versionCode = 2, libVersion = LibVersion(1, 4))))
        // A higher lib doesn't make up for a lower version code: the install would refuse it
        assertFalse(installed.isUpdatedBy(available(versionCode = 1, libVersion = lib16)))
    }

    @Test
    fun anUnknownLibIsNoUpdateAndIsUpdatedByAKnownOne() {
        assertFalse(Extension.isUpdate(2, null, 2, lib15))
        assertFalse(Extension.isUpdate(2, null, 2, null))
        assertTrue(Extension.isUpdate(2, lib15, 2, null))
        assertTrue(Extension.isUpdate(3, null, 2, lib15))
    }
}
