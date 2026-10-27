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
