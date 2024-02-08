package com.sf.tadami.preferences.model

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.datastore.preferences.core.Preferences

sealed class SourcePreference {
    abstract val title: String
    abstract val enabled : Boolean

    sealed class PreferenceItem<T> : SourcePreference() {
        abstract val subtitle : String?
        abstract val icon : ImageVector?
        abstract val onValueChanged : (newValue : T) -> Boolean

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
            fun castedOnValueChanged(newValue : Any) = onValueChanged(newValue as S)
        }

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
            val subtitleProvider : () -> String? = {
               ""
            },
        ) : PreferenceItem<Set<String>>()

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
    }

    data class PreferenceCategory(
        override val title: String,
        override val enabled: Boolean = true,
        val preferenceItems : List<PreferenceItem<*>> = emptyList()
    ) : SourcePreference()

}