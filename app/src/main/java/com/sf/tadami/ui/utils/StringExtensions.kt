package com.sf.tadami.ui.utils

fun String.capFirstLetter() : String{
    return this.replaceFirstChar { it.uppercase() }
}

fun String.lowFirstLetter() : String{
    return this.replaceFirstChar { it.lowercase() }
}

fun String.chop(count: Int, replacement: String = "…"): String {
    return if (length > count) {
        take(count - replacement.length) + replacement
    } else {
        this
    }
}