package com.sf.tadami.preferences.extensions

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.sf.tadami.preferences.model.CustomPreferences
import com.sf.tadami.preferences.model.CustomPreferencesIdentifier

data class TrustedExtensionsPreferences(
    /** SHA-256 fingerprints of the signing keys the user trusted, besides the official one. */
    val trustedSignatures: Set<String>,
) : CustomPreferencesIdentifier {

    companion object : CustomPreferences<TrustedExtensionsPreferences> {
        // Private, so neither backed up nor restored: a backup file can't make an extension trusted
        val TRUSTED_SIGNATURES =
            stringSetPreferencesKey(CustomPreferences.privateKey("trusted_extension_signatures"))

        override fun transform(preferences: Preferences): TrustedExtensionsPreferences {
            return TrustedExtensionsPreferences(
                trustedSignatures = preferences[TRUSTED_SIGNATURES] ?: emptySet(),
            )
        }

        override fun setPrefs(newValue: TrustedExtensionsPreferences, preferences: MutablePreferences) {
            preferences[TRUSTED_SIGNATURES] = newValue.trustedSignatures
        }
    }
}
