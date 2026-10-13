package com.sf.tadami.extension.api

import com.sf.tadami.domain.extensions.Extension
import com.sf.tadami.extension.model.LibVersion
import com.sf.tadami.extension.util.ExtensionsLoader
import com.sf.tadami.utils.Lang
import org.junit.Assert.assertEquals
import org.junit.Test

class RepoExtensionsTest {

    private fun available(pkgName: String, libVersion: LibVersion) = Extension.Available(
        name = pkgName,
        pkgName = pkgName,
        versionName = "$libVersion.1",
        versionCode = 1,
        apiVersion = libVersion,
        lang = Lang.ENGLISH,
        sources = emptyList(),
        apkName = "$pkgName.apk",
        iconUrl = "",
        repoUrl = "",
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
}
