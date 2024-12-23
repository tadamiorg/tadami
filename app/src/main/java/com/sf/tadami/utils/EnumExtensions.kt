package com.sf.tadami.utils

inline fun <reified T : Enum<T>> enumValueOfOrNull(name: String?): T? {
    return try {
        enumValueOf<T>(name as String)
    } catch (_: Exception) {
        null
    }
}