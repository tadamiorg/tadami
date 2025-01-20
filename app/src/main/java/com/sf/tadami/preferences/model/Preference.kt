package com.sf.tadami.preferences.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.sf.tadami.R
import com.sf.tadami.preferences.model.SourcePreference.PreferenceItem

sealed class Preference {
    abstract val title: String
    abstract val enabled : Boolean

    sealed class PreferenceItem<T> : Preference() {
        abstract val subtitle : String?
        abstract val icon : ImageVector?
        abstract val onValueChanged : (newValue : T) -> Boolean

        data class TogglePreference(
            var value : Boolean,
            override val title: String,
            override val subtitle: String? = null,
            override val icon: ImageVector? = null,
            override val enabled: Boolean = true,
            override val onValueChanged: (newValue: Boolean) -> Boolean = { true }
        ) : PreferenceItem<Boolean>()

        @Suppress("UNCHECKED_CAST")
        data class SelectPreference<S : Any>(
            var value : S,
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
            var value : Set<String>,
            var items : Map<String,Pair<String,Boolean>>,
            override val title: String,
            override val subtitle: String? = "%s",
            override val icon: ImageVector? = null,
            override val enabled: Boolean = true,
            val overrideOkButton : Boolean = false,
            val subtitleProvider : @Composable () -> String? = {
                val values = value.map{items[it]}.takeIf { it.isNotEmpty() }?.joinToString{ it!!.first } ?: stringResource(id = R.string.none)
                subtitle?.format(values)
            },
            override val onValueChanged: (newValue: Set<String>) -> Boolean = { true }
        ) : PreferenceItem<Set<String>>()

        data class MultiSelectPreferenceInt(
            var value : Set<Int>,
            var items : Map<Int,Pair<String,Boolean>>,
            override val title: String,
            override val subtitle: String? = "%s",
            override val icon: ImageVector? = null,
            override val enabled: Boolean = true,
            val overrideOkButton : Boolean = false,
            val subtitleProvider : @Composable () -> String? = {
                val values = value.map{items[it]}.takeIf { it.isNotEmpty() }?.joinToString{ it!!.first } ?: stringResource(id = R.string.none)
                subtitle?.format(values)
            },
            override val onValueChanged: (newValue: Set<Int>) -> Boolean = { true }
        ) : PreferenceItem<Set<Int>>()

        data class TextPreference(
            override val title: String,
            override val subtitle: String? = null,
            override val icon: ImageVector? = null,
            override val onValueChanged: (newValue: String) -> Boolean = { true },
            override val enabled: Boolean = true,
            val onClick : (() -> Unit)? = null
        ) : PreferenceItem<String>()

        data class EditTextPreference(
            val value: String,
            val defaultValue : String? = null,
            override val title: String,
            override val subtitle: String? = "%s",
            override val icon: ImageVector? = null,
            override val enabled: Boolean = true,
            override val onValueChanged: (newValue: String) -> Boolean = { true },
        ) : PreferenceItem<String>()

        data class CustomPreference(
            override val title: String,
            val content: @Composable (PreferenceItem<String>) -> Unit,
        ) : PreferenceItem<String>() {
            override val enabled: Boolean = true
            override val subtitle: String? = null
            override val icon: ImageVector? = null
            override val onValueChanged: (newValue: String) -> Boolean = { true }
        }

        data class InfoPreference(
            override val title: String,
        ) : PreferenceItem<String>() {
            override val enabled: Boolean = true
            override val subtitle: String? = null
            override val icon: ImageVector? = null
            override val onValueChanged: (newValue: String) -> Boolean = { true }
        }

        data class ReorderStringPreference(
            var value : String,
            var items : Map<String,String>,
            override val title: String,
            override val subtitle: String? = "%s",
            override val icon: ImageVector? = null,
            override val enabled: Boolean = true,
            val overrideOkButton : Boolean = false,
            override val onValueChanged: (newValue: String) -> Boolean = { true },
        ) : PreferenceItem<String>()
    }

    data class PreferenceCategory(
        override val title: String,
        override val enabled: Boolean = true,
        val preferenceItems : List<PreferenceItem<*>> = emptyList()
    ) : Preference()

}
