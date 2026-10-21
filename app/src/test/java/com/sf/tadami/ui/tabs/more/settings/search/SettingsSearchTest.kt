package com.sf.tadami.ui.tabs.more.settings.search

import com.sf.tadami.preferences.model.Preference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsSearchTest {

    private fun entry(title: String, summary: String? = null, group: String? = "Group") =
        SettingsSearchEntry(route = "route", screenTitle = "Screen", group = group, title = title, summary = summary)

    private fun List<SettingsSearchEntry>.titles() = map { it.title }

    @Test
    fun normalizationDropsCaseAccentsAndPunctuation() {
        assertEquals("preferences wifi", SettingsSearch.normalize("  Préférences  Wi-Fi! "))
        assertEquals("ecran telechargement", SettingsSearch.normalize("ÉCRAN — Téléchargement"))
    }

    @Test
    fun caseAndAccentsAreIgnored() {
        val index = listOf(entry("Télécharger uniquement en Wi-Fi"), entry("Thème"))
        assertEquals(listOf("Télécharger uniquement en Wi-Fi"), SettingsSearch.search(index, "TELECHARGER").titles())
        assertEquals(listOf("Thème"), SettingsSearch.search(index, "théme").titles())
    }

    @Test
    fun punctuationAndSpacesAreIgnored() {
        val index = listOf(entry("Only download over Wi-Fi"), entry("Block ads in the WebView"))
        for (query in listOf("wifi", "wi-fi", "Wi Fi", "WIFI")) {
            assertEquals(query, listOf("Only download over Wi-Fi"), SettingsSearch.search(index, query).titles())
        }
        assertEquals(listOf("Block ads in the WebView"), SettingsSearch.search(index, "web view").titles())
    }

    @Test
    fun wordsMatchWithinOneWordOfTheText() {
        val index = listOf(entry("Block ads", summary = "With the filter lists below"), entry("Only download over Wi-Fi"))
        assertEquals(listOf("Only download over Wi-Fi"), SettingsSearch.search(index, "wi-fi").titles())
        assertEquals(listOf("Block ads"), SettingsSearch.search(index, "filter with").titles())
        assertTrue(SettingsSearch.search(index, "kad").isEmpty())
    }

    @Test
    fun everyWordMustMatchInAnyOrder() {
        val index = listOf(entry("WebView ad blocker"), entry("Block ads in the WebView"), entry("Clear WebView data"))
        assertEquals(
            listOf("WebView ad blocker", "Block ads in the WebView"),
            SettingsSearch.search(index, "ad block").titles()
        )
        assertEquals(listOf("Clear WebView data"), SettingsSearch.search(index, "data webview").titles())
        assertTrue(SettingsSearch.search(index, "ad cookies").isEmpty())
    }

    @Test
    fun theSummaryIsSearchedToo() {
        val index = listOf(entry("Auto-refresh metadata", summary = "Check for new covers and details"))
        assertEquals(listOf("Auto-refresh metadata"), SettingsSearch.search(index, "covers").titles())
        // A word of the title and one of the summary
        assertEquals(listOf("Auto-refresh metadata"), SettingsSearch.search(index, "metadata covers").titles())
    }

    @Test
    fun titleMatchesComeFirst() {
        val bySummary = entry("Auto-refresh metadata", summary = "Check for new covers of the library")
        val byWords = entry("Covers of the library to refresh")
        val byPhrase = entry("Refresh library covers and details")
        val index = listOf(bySummary, byWords, byPhrase)
        assertEquals(listOf(byPhrase, byWords, bySummary), SettingsSearch.search(index, "library covers"))
        // The same rank keeps the order of the index
        assertEquals(listOf(byWords, byPhrase, bySummary), SettingsSearch.search(index, "covers"))
    }

    @Test
    fun aBlankQueryFindsNothing() {
        val index = listOf(entry("Theme"))
        assertTrue(SettingsSearch.search(index, "").isEmpty())
        assertTrue(SettingsSearch.search(index, " - ").isEmpty())
    }

    @Test
    fun theBreadcrumbIsTheScreenThenTheGroup() {
        assertEquals("Screen > Group", entry("Theme").breadcrumb)
        assertEquals("Screen", entry("Theme", group = null).breadcrumb)
    }

    @Test
    fun theHeadingsAndVisibleItemsAreSearchable() {
        val preferences = listOf(
            Preference.PreferenceItem.TextPreference(title = "Storage location"),
            Preference.PreferenceItem.InfoPreference(title = "A note about the screen"),
            Preference.PreferenceCategory(
                title = "Network",
                preferenceItems = listOf(
                    Preference.PreferenceItem.TextPreference(title = "Network"),
                    Preference.PreferenceItem.TextPreference(title = "Clear cookies"),
                    Preference.PreferenceItem.TextPreference(title = "Reset user agent", enabled = false),
                    Preference.PreferenceItem.TogglePreference(value = true, title = ""),
                    Preference.PreferenceItem.InfoPreference(title = "Info"),
                    Preference.PreferenceItem.TogglePreference(value = true, title = "Block ads"),
                )
            ),
        )
        assertEquals(
            listOf(null to "Storage location", null to "Network", "Network" to "Clear cookies", "Network" to "Block ads"),
            preferences.searchablePreferences().map { it.group to it.preference.title }
        )
    }
}
