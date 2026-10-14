package com.sf.tadami.extension.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtensionsMigrationTest {

    @Test
    fun anAppIsCopiedWithoutACopyOrWhenNewerThanIt() {
        assertTrue(ExtensionsMigration.shouldAdopt(installedVersionCode = 3, privateVersionCode = null))
        assertTrue(ExtensionsMigration.shouldAdopt(installedVersionCode = 3, privateVersionCode = 2))
        assertFalse(ExtensionsMigration.shouldAdopt(installedVersionCode = 3, privateVersionCode = 3))
        assertFalse(ExtensionsMigration.shouldAdopt(installedVersionCode = 3, privateVersionCode = 4))
    }
}
