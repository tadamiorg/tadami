package com.sf.tadami.preferences.extensions

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.sf.tadami.App
import com.sf.tadami.preferences.model.CustomPreferences
import com.sf.tadami.preferences.model.CustomPreferencesIdentifier
import com.sf.tadami.utils.hasMiuiPackageInstaller
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

data class ExtensionsPreferences(
    val extensionInstallerEnum : ExtensionInstallerEnum
) : CustomPreferencesIdentifier {

    companion object : CustomPreferences<ExtensionsPreferences> {
        val EXTENSION_INSTALLER =  stringPreferencesKey("extension_installer")

        private fun getDefaultInstaller(context : Context) : ExtensionInstallerEnum{
            return if (context.hasMiuiPackageInstaller()) {
                ExtensionInstallerEnum.LEGACY
            } else {
                ExtensionInstallerEnum.PACKAGEINSTALLER
            }
        }
        fun getInstallerEntries(context : Context) : List<ExtensionInstallerEnum>{
            return ExtensionInstallerEnum.entries.run {
                if (context.hasMiuiPackageInstaller()) {
                    filter { it != ExtensionInstallerEnum.PACKAGEINSTALLER }
                } else {
                    toList()
                }
            }
        }

        override fun transform(preferences: Preferences): ExtensionsPreferences {
            val context = App.getAppContext()!!
            return ExtensionsPreferences(
                extensionInstallerEnum = preferences[EXTENSION_INSTALLER]?.let{
                    ExtensionInstallerEnum.valueOf(it)
                } ?: getDefaultInstaller(context)
            )
        }

        override fun setPrefs(newValue: ExtensionsPreferences, preferences: MutablePreferences) {
            preferences[EXTENSION_INSTALLER] = newValue.extensionInstallerEnum.name
        }
    }
}