package com.sf.tadami.preferences.extensions

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.sf.tadami.preferences.model.CustomPreferences
import com.sf.tadami.preferences.model.CustomPreferencesIdentifier

data class SourceIdOwnersPreferences(
    /** The extension, a package name, owning each source ID, see resolveSourceIdConflicts. */
    val owners: Map<Long, String>,
    /**
     * The extension that registered the stub of each source ID, the name its downloads are stored
     * under: renamed with its source only, see SourceManager.
     */
    val stubOwners: Map<Long, String>,
) : CustomPreferencesIdentifier {

    companion object : CustomPreferences<SourceIdOwnersPreferences> {
        // Private, so neither backed up nor restored: they describe the extensions of this device
        val OWNERS = stringSetPreferencesKey(CustomPreferences.privateKey("source_id_owners"))
        val STUB_OWNERS = stringSetPreferencesKey(CustomPreferences.privateKey("source_stub_owners"))

        override fun transform(preferences: Preferences): SourceIdOwnersPreferences {
            return SourceIdOwnersPreferences(
                owners = decode(preferences[OWNERS]),
                stubOwners = decode(preferences[STUB_OWNERS]),
            )
        }

        override fun setPrefs(newValue: SourceIdOwnersPreferences, preferences: MutablePreferences) {
            preferences[OWNERS] = encode(newValue.owners)
            preferences[STUB_OWNERS] = encode(newValue.stubOwners)
        }

        /** "<sourceId>=<pkgName>" entries. */
        fun encode(owners: Map<Long, String>): Set<String> = owners.map { (id, pkgName) -> "$id=$pkgName" }.toSet()

        fun decode(entries: Set<String>?): Map<Long, String> = entries.orEmpty().mapNotNull { entry ->
            val id = entry.substringBefore('=').toLongOrNull() ?: return@mapNotNull null
            val pkgName = entry.substringAfter('=', "").takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            id to pkgName
        }.toMap()
    }
}
