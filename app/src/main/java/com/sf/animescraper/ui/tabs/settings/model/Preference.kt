package com.sf.animescraper.ui.tabs.settings.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.sf.animescraper.R

sealed class Preference {
    abstract val title: String

    sealed class PreferenceItem<T> : Preference() {
        abstract val subtitle : String?
        abstract val icon : ImageVector?
        abstract val onValueChanged : (newValue : T) -> Boolean

        data class TogglePreference(
            var value : Boolean,
            override val title: String,
            override val subtitle: String? = null,
            override val icon: ImageVector? = null,
            override val onValueChanged: (newValue: Boolean) -> Boolean = { true }
        ) : PreferenceItem<Boolean>()

        @Suppress("UNCHECKED_CAST")
        data class SelectPreference<S : Any>(
            var value : S,
            var items : Map<S,String>,
            override val title: String,
            override val subtitle: String? = "%s",
            override val icon: ImageVector? = null,
            override val onValueChanged: (newValue: S) -> Boolean = { true },
            val subtitleProvider : () -> String? = {
                subtitle?.format(items[value])
            }
        ) : PreferenceItem<S>(){
            fun castedOnValueChanged(newValue : Any) = onValueChanged(newValue as S)
        }

        @Suppress("UNCHECKED_CAST")
        data class MultiSelectPreference(
            var value : Set<String>,
            var items : Map<String,String>,
            override val title: String,
            override val subtitle: String? = "%s",
            override val icon: ImageVector? = null,
            val subtitleProvider : @Composable () -> String? = {
                val values = value.map{items[it]}.takeIf { it.isNotEmpty() }?.joinToString() ?: stringResource(id = R.string.none)
                subtitle?.format(values)
            },
            override val onValueChanged: (newValue: Set<String>) -> Boolean = { true }
        ) : PreferenceItem<Set<String>>()

        data class TextPreference(
            override val title: String,
            override val subtitle: String? = null,
            override val icon: ImageVector? = null,
            override val onValueChanged: (newValue: String) -> Boolean = { true },
            val onClick : (() -> Unit)? = null
        ) : PreferenceItem<String>()
    }

    data class PreferenceCategory(
        override val title: String,
        val preferenceItems : List<PreferenceItem<*>> = emptyList()
    ) : Preference()

}
