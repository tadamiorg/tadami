package com.sf.tadami.ui.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking

// IO rather than Default: sources call blocking network code (and may wait on a WebView) here
fun <A, B> Iterable<A>.parallelMap(f: suspend (A) -> B): List<B> =
    runBlocking {
        map { async(Dispatchers.IO) { f(it) } }.awaitAll()
    }

fun <A, B> Iterable<A>.parallelMapIndexed(f: suspend (Int,A) -> B): List<B> =
    runBlocking {
        mapIndexed { index,it -> async(Dispatchers.IO) { f(index,it) } }.awaitAll()
    }