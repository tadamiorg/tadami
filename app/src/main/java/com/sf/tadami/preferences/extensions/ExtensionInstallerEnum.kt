package com.sf.tadami.preferences.extensions

import androidx.annotation.StringRes
import com.sf.tadami.R

enum class ExtensionInstallerEnum(@StringRes val titleRes: Int, val requiresSystemPermission: Boolean) {
    LEGACY(R.string.stub_text, true),
    PACKAGEINSTALLER(R.string.stub_text, true);

    companion object{
        fun nullableValueOf(value : String) : ExtensionInstallerEnum?{
            return try {
                ExtensionInstallerEnum.valueOf(value)
            }catch (e : Exception){
                null
            }
        }
    }
}