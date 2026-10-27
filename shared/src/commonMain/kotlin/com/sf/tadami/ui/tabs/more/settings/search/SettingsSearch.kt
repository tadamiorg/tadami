package com.sf.tadami.ui.tabs.more.settings.search

import com.sf.tadami.preferences.model.Preference
import java.text.Normalizer
import java.util.Locale

/** A setting the search finds: a preference, or the heading of a group, of a settings screen */
data class SettingsSearchEntry(
    val route: String,
    val screenTitle: String,
    val group: String?,
    val title: String,
    val summary: String? = null,
) {
    val breadcrumb: String
        get() = if (group == null) screenTitle else "$screenTitle > $group"
}

/** What a settings screen shows, as the search finds it: its group headings and their visible items */
data class SearchablePreference(val group: String?, val preference: Preference)

/** The rows of a settings screen the search can lead to, in the order the screen shows them */
fun List<Preference>.searchablePreferences(): List<SearchablePreference> = buildList {
    for (preference in this@searchablePreferences) {
        if (preference.title.isBlank()) continue
        when (preference) {
            is Preference.PreferenceCategory -> {
                add(SearchablePreference(null, preference))
                preference.preferenceItems
                    // One titled like its group is found, and highlighted, with the heading
                    .filter { it.isSearchable() && it.title != preference.title }
                    .forEach { add(SearchablePreference(preference.title, it)) }
            }

            is Preference.PreferenceItem<*> -> {
                if (preference.isSearchable()) add(SearchablePreference(null, preference))
            }
        }
    }
}

// A disabled item is hidden, and an info is a note about the screen rather than a setting
private fun Preference.PreferenceItem<*>.isSearchable(): Boolean =
    enabled && title.isNotBlank() && this !is Preference.PreferenceItem.InfoPreference

object SettingsSearch {
    private val MARKS = "\\p{Mn}+".toRegex()
    private val PUNCTUATION = "[^\\p{L}\\p{N}\\s]+".toRegex()
    private val SPACES = "\\s+".toRegex()

    /** Lowercase, without accents nor punctuation: "Préférences  Wi-Fi !" is "preferences wifi" */
    fun normalize(text: String): String {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(MARKS, "")
            .lowercase(Locale.ROOT)
            .replace(PUNCTUATION, "")
            .replace(SPACES, " ")
            .trim()
    }

    /**
     * The entries holding the query in their title or their summary, ignoring case, accents and
     * punctuation: as typed, or without its spaces within a word ("wifi", "wi-fi" and "wi fi" find
     * "Wi-Fi only"), or each of its words within a word ("ad block" finds "Block ads").
     * The entries with the query in their title come first, then those with all its words in their
     * title, then those found by their summary, each in the order of the index.
     */
    fun search(index: List<SettingsSearchEntry>, query: String): List<SettingsSearchEntry> {
        val normalized = normalize(query)
        if (normalized.isEmpty()) return emptyList()
        val words = normalized.split(' ')
        val phrase = words.joinToString("")
        return index.mapNotNull { entry ->
            val title = Text(entry.title)
            val summary = Text(entry.summary.orEmpty())
            val rank = when {
                normalized in title.text || title.hasWord(phrase) -> 0
                words.all { title.hasWord(it) } -> 1
                normalized in summary.text || summary.hasWord(phrase) -> 2
                words.all { title.hasWord(it) || summary.hasWord(it) } -> 2
                else -> return@mapNotNull null
            }
            rank to entry
        }.sortedBy { it.first }.map { it.second }
    }

    private class Text(text: String) {
        val text = normalize(text)
        private val words = this.text.split(' ')

        // Within one word: "wi fi" isn't "with filters"
        fun hasWord(word: String) = words.any { word in it }
    }
}
