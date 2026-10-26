package com.sf.tadami.ui.utils

import java.util.Locale

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

fun String.convertToIetfLanguageTag(): String {
    val input = this.lowercase(Locale.ROOT)
    return when {
        "english" in input -> Locale.ENGLISH.toLanguageTag()
        "french" in input -> Locale.FRENCH.toLanguageTag()
        "spanish" in input -> Locale("es").toLanguageTag() // General Spanish
        "portuguese" in input -> Locale("pt").toLanguageTag() // General Portuguese
        "german" in input -> Locale.GERMAN.toLanguageTag()
        "russian" in input -> Locale("ru").toLanguageTag()
        "italian" in input -> Locale.ITALIAN.toLanguageTag()
        "arabic" in input -> Locale("ar").toLanguageTag()
        else -> this
    }
}
