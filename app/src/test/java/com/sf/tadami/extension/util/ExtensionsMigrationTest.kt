package com.sf.tadami.extension.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtensionsMigrationTest {

    private fun shouldAdopt(privateVersionCode: Long?, debug: Boolean = false, isSameFile: Boolean = false) =
        ExtensionsMigration.shouldAdopt(installedVersionCode = 3, privateVersionCode, debug) { isSameFile }

    @Test
    fun anAppIsCopiedWithoutACopyOrWhenNewerThanIt() {
        assertTrue(shouldAdopt(privateVersionCode = null))
        assertTrue(shouldAdopt(privateVersionCode = 2))
        assertFalse(shouldAdopt(privateVersionCode = 3))
        assertFalse(shouldAdopt(privateVersionCode = 4))
    }

    @Test
    fun debugBuildsCopyTheSameVersionReinstalledWithAdb() {
        assertTrue(shouldAdopt(privateVersionCode = 3, debug = true, isSameFile = false))
        assertFalse(shouldAdopt(privateVersionCode = 3, debug = true, isSameFile = true))
        assertFalse(shouldAdopt(privateVersionCode = 4, debug = true, isSameFile = false))
    }
}
