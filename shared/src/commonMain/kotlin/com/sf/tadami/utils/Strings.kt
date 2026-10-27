package com.sf.tadami.utils

import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString

/**
 * [res] formatted with [args], for the code outside composition that can't suspend: Context.getString
 * of the Android code. The strings are cached once read.
 */
fun stringRes(res: StringResource, vararg args: Any): String = runBlocking { getString(res, *args) }

/** The plural [res] for [quantity] formatted with [args], see [stringRes]. */
fun pluralRes(res: PluralStringResource, quantity: Int, vararg args: Any): String =
    runBlocking { getPluralString(res, quantity, *args) }

/** [this] encoded for a URI component, as Android's Uri.encode: everything but the unreserved characters. */
fun String.encodeUriComponent(): String =
    java.net.URLEncoder.encode(this, "UTF-8").replace("+", "%20").replace("%7E", "~")
        .replace("%21", "!").replace("%27", "'").replace("%28", "(").replace("%29", ")").replace("%2A", "*")

/** [this] decoded from a URI component, see [encodeUriComponent]. */
fun String.decodeUriComponent(): String = java.net.URLDecoder.decode(replace("+", "%2B"), "UTF-8")
