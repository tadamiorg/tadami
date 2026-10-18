package com.sf.tadami.preferences.model

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.datastore.preferences.core.Preferences

/**
 * An entry of the settings screen of a source, see
 * [com.sf.tadami.source.ConfigurableSource.getPreferenceScreen]. The app stores the value of each
 * item under its key, in the [com.sf.tadami.source.ConfigurableSource.dataStore] of the source.
 *
 * @property title shown in the screen
 * @property enabled whether the user can change it
 * @since extensions-lib 1.5
 */
sealed class SourcePreference {
    abstract val title: String
    abstract val enabled : Boolean

    /**
     * A preference of type [T] the user edits.
     *
     * @property subtitle shown under the title. In the items that have a `%s` subtitle by default,
     * `%s` is replaced by the current value
     * @property icon shown before the title
     * @property onValueChanged called with the value the user chose, once stored
     * @since extensions-lib 1.5
     */
    sealed class PreferenceItem<T> : SourcePreference() {
        abstract val subtitle : String?
        abstract val icon : ImageVector?
        abstract val onValueChanged : (newValue : T) -> Boolean

        /**
         * A switch.
         *
         * @property value the value when the screen was built: the app shows the one stored under [key]
         * @property defaultValue the value while [key] isn't set
         * @since extensions-lib 1.5
         */
        data class TogglePreference(
            var value : Boolean,
            val key : Preferences.Key<Boolean>,
            val defaultValue: Boolean,
            override val title: String,
            override val subtitle: String? = null,
            override val icon: ImageVector? = null,
            override val enabled: Boolean = true,
            override val onValueChanged: (newValue: Boolean) -> Boolean = { true }

        ) : PreferenceItem<Boolean>()

        /**
         * A choice of one of [items].
         *
         * @property value the value when the screen was built: the app shows the one stored under [key]
         * @property defaultValue the value while [key] isn't set
         * @property items the values, with their labels
         * @property subtitleProvider the subtitle shown, by default [subtitle] formatted with the
         * label of the value
         * @since extensions-lib 1.5
         */
        @Suppress("UNCHECKED_CAST")
        data class SelectPreference<S : Any>(
            var value : S,
            val key : Preferences.Key<S>,
            val defaultValue: S,
            var items : Map<S,String>,
            override val title: String,
            override val subtitle: String? = "%s",
            override val icon: ImageVector? = null,
            override val enabled: Boolean = true,
            override val onValueChanged: (newValue: S) -> Boolean = { true },
            val subtitleProvider : () -> String? = {
                subtitle?.format(items[value])
            }
        ) : PreferenceItem<S>(){
            /**
             * [onValueChanged] of a value of an unknown type.
             *
             * @since extensions-lib 1.5
             */
            fun castedOnValueChanged(newValue : Any) = onValueChanged(newValue as S)
        }

        /**
         * A choice of several of [items].
         *
         * @property value the value when the screen was built: the app shows the one stored under [key]
         * @property items the values, with their label and whether the user can pick them
         * @property defaultValue the value while [key] isn't set
         * @property overrideOkButton whether the dialog has no OK button, every pick being applied
         * @property subtitleProvider the subtitle shown for a value, by default [subtitle]
         * formatted with the labels of the value
         * @since extensions-lib 1.5
         */
        data class MultiSelectPreference(
            val key : Preferences.Key<Set<String>>,
            var value : Set<String>,
            var items : Map<String,Pair<String,Boolean>>,
            val defaultValue: Set<String>,
            override val title: String,
            override val subtitle: String? = "%s",
            override val icon: ImageVector? = null,
            override val enabled: Boolean = true,
            val overrideOkButton : Boolean = false,
            override val onValueChanged: (newValue: Set<String>) -> Boolean = { true },
            val subtitleProvider : (currentValue : Set<String>) -> String? = throw Exception("Stub !"),
        ) : PreferenceItem<Set<String>>()

        /**
         * A text field.
         *
         * @property value the value when the screen was built: the app shows the one stored under [key]
         * @property defaultValue the value while [key] isn't set
         * @since extensions-lib 1.5
         */
        data class EditTextPreference(
            var value: String,
            val key : Preferences.Key<String>,
            val defaultValue : String,
            override val title: String,
            override val subtitle: String? = "%s",
            override val icon: ImageVector? = null,
            override val enabled: Boolean = true,
            override val onValueChanged: (newValue: String) -> Boolean = { true },
        ) : PreferenceItem<String>()

        /**
         * An order of the keys of [items] the user sorts, stored comma-separated.
         *
         * @property value the value when the screen was built: the app shows the one stored under [key]
         * @property items the keys, with their labels
         * @property defaultValue the order while [key] isn't set, comma-separated
         * @property overrideOkButton whether the dialog has no OK button
         * @since extensions-lib 1.5
         */
        data class ReorderStringPreference(
            val key : Preferences.Key<String>,
            var value : String,
            var items : Map<String,String>,
            val defaultValue: String,
            override val title: String,
            override val subtitle: String? = "%s",
            override val icon: ImageVector? = null,
            override val enabled: Boolean = true,
            val overrideOkButton : Boolean = false,
            override val onValueChanged: (newValue: String) -> Boolean = { true },
        ) : PreferenceItem<String>()
    }

    /**
     * A titled group of [preferenceItems].
     *
     * @property videoCategory whether the player also shows it, in its settings
     * @since extensions-lib 1.5
     */
    data class PreferenceCategory(
        override val title: String,
        override val enabled: Boolean = true,
        val videoCategory : Boolean = false,
        val preferenceItems : List<PreferenceItem<*>> = emptyList()
    ) : SourcePreference()

}
