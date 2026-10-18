package com.sf.tadami.ui.discover.migrate.dialog

import com.sf.tadami.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MigrationFlagsTest {

    @Test
    fun theCategoriesAreCopiedByDefault() {
        val flags = MigrationFlags.getFlags(hasDownloads = false)

        val categories = flags.single { it.titleId == R.string.label_categories }
        assertTrue(categories.isDefaultSelected)

        val defaults = MigrationFlags.getSelectedFlagsBitMap(flags.map { it.isDefaultSelected }, flags)
        assertTrue(MigrationFlags.hasCategories(defaults))
        assertTrue(MigrationFlags.hasEpisodes(defaults))
    }

    @Test
    fun theCategoriesFlagIsItsOwn() {
        val flags = MigrationFlags.getFlags(hasDownloads = true)
        val onlyCategories = flags.map { it.titleId == R.string.label_categories }

        val value = MigrationFlags.getSelectedFlagsBitMap(onlyCategories, flags)

        assertTrue(MigrationFlags.hasCategories(value))
        assertFalse(MigrationFlags.hasEpisodes(value))
        assertFalse(MigrationFlags.hasDeleteDownloaded(value))
    }

    @Test
    fun theCategoriesCanBeLeftBehind() {
        val flags = MigrationFlags.getFlags(hasDownloads = false)
        val withoutCategories = flags.map { it.titleId != R.string.label_categories }

        val value = MigrationFlags.getSelectedFlagsBitMap(withoutCategories, flags)

        assertFalse(MigrationFlags.hasCategories(value))
        assertEquals(listOf(R.string.label_episodes, R.string.label_categories), flags.map { it.titleId })
    }
}
