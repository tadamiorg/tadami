package com.sf.tadami.utils

import android.annotation.SuppressLint
import android.util.Log

object DeviceUtil {
    val isMiui: Boolean by lazy {
        getSystemProperty("ro.miui.ui.version.name")?.isNotEmpty() ?: false
    }
    val invalidDefaultBrowsers = listOf(
        "android",
        "com.huawei.android.internal.app",
        "com.zui.resolver",
    )

    @SuppressLint("PrivateApi")
    private fun getSystemProperty(key: String?): String? {
        return try {
            Class.forName("android.os.SystemProperties")
                .getDeclaredMethod("get", String::class.java)
                .invoke(null, key) as String
        } catch (e: Exception) {
            Log.w("GetSystemProperty","Unable to use SystemProperties.get()",e)
            null
        }
    }

    @SuppressLint("PrivateApi")
    fun isMiuiOptimizationDisabled(): Boolean {
        val sysProp = getSystemProperty("persist.sys.miui_optimization")
        if (sysProp == "0" || sysProp == "false") {
            return true
        }

        return try {
            Class.forName("android.miui.AppOpsUtils")
                .getDeclaredMethod("isXOptMode")
                .invoke(null) as Boolean
        } catch (e: Exception) {
            false
        }
    }
}