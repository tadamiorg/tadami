package com.sf.tadami.ui.tabs.more.settings.search

import com.sf.tadami.ui.tabs.more.settings.components.PreferenceScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.lang.reflect.Modifier
import java.util.jar.JarFile

class SearchableSettingsTest {

    /** Every class of the app that is a settings screen */
    private fun preferenceScreens(): Set<Class<*>> {
        val root = File(PreferenceScreen::class.java.protectionDomain!!.codeSource.location.toURI())
        val classFiles = if (root.isDirectory) {
            root.walk().filter { it.isFile && it.extension == "class" }.map { it.relativeTo(root).invariantSeparatorsPath }.toList()
        } else {
            JarFile(root).use { jar -> jar.entries().asSequence().map { it.name }.filter { it.endsWith(".class") }.toList() }
        }
        val loader = PreferenceScreen::class.java.classLoader
        return classFiles.asSequence()
            .map { it.removeSuffix(".class").replace('/', '.') }
            .filter { it.startsWith("com.sf.tadami.") }
            .mapNotNull { name -> runCatching { Class.forName(name, false, loader) }.getOrNull() }
            .filter { PreferenceScreen::class.java.isAssignableFrom(it) && !it.isInterface && !Modifier.isAbstract(it.modifiers) }
            .toSet()
    }

    @Test
    fun everySettingsScreenIsSearchableOrKnownNotToBe() {
        val screens = preferenceScreens()
        // The search would be untested if the scan found nothing
        assertTrue(screens.size >= SearchableSettings.screens.size)

        val known = SearchableSettings.screens.map { it.type.java } + SearchableSettings.notSearchable.map { it.java }
        val missing = screens - known.toSet()
        assertTrue(
            "Add these settings screens to SearchableSettings.screens, or to notSearchable: ${missing.map { it.name }}",
            missing.isEmpty()
        )
        assertEquals("Known screens that no longer exist", emptySet<Class<*>>(), known.toSet() - screens)
    }

    @Test
    fun eachScreenIsListedOnce() {
        val searchable = SearchableSettings.screens
        assertEquals(searchable.size, searchable.map { it.type }.distinct().size)
        assertEquals(searchable.size, searchable.map { it.route }.distinct().size)
        assertTrue(searchable.none { it.type in SearchableSettings.notSearchable })
    }
}
