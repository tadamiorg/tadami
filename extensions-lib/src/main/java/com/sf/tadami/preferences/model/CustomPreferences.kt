package com.sf.tadami.preferences.model

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences

/**
 * Reads and writes a group of preferences of type [T] in a DataStore, usually the companion object
 * of [T]: see [com.sf.tadami.utils.getPreferencesGroup] and [com.sf.tadami.utils.editPreferences].
 *
 * @since extensions-lib 1.5
 */
interface CustomPreferences<T> {
    /**
     * The group read from [preferences], with the defaults of the keys not set.
     *
     * @since extensions-lib 1.5
     */
    fun transform(preferences: Preferences) : T

    /**
     * Writes [newValue] in [preferences].
     *
     * @since extensions-lib 1.5
     */
    fun setPrefs(newValue : T,preferences : MutablePreferences)

    companion object{
        /**
         * Whether [key] was made by [privateKey].
         *
         * @since extensions-lib 1.5
         */
        fun isPrivate(key: String): Boolean = throw Exception("Stub")

        /**
         * [key] made private: never backed up nor restored.
         *
         * @since extensions-lib 1.5
         */
        fun privateKey(key: String): String = throw Exception("Stub")

        /**
         * Whether [key] was made by [appStateKey].
         *
         * @since extensions-lib 1.5
         */
        fun isAppState(key: String): Boolean = throw Exception("Stub")

        /**
         * [key] made an app state, which isn't a setting: not backed up.
         *
         * @since extensions-lib 1.5
         */
        fun appStateKey(key: String): String = throw Exception("Stub")
        private const val APP_STATE_PREFIX = "__APP_STATE_"
        private const val PRIVATE_PREFIX = "__PRIVATE_"
    }
}

/**
 * A group of preferences, read and written by its [CustomPreferences].
 *
 * @since extensions-lib 1.5
 */
interface CustomPreferencesIdentifier {}
