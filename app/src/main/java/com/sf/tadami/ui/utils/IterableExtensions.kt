package com.sf.tadami.ui.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking

fun <A, B> Iterable<A>.parallelMap(f: suspend (A) -> B): List<B> =
    runBlocking {
        map { async(Dispatchers.Default) { f(it) } }.awaitAll()
    }

fun <A, B> Iterable<A>.parallelMapIndexed(f: suspend (Int,A) -> B): List<B> =
    runBlocking {
        mapIndexed { index,it -> async(Dispatchers.Default) { f(index,it) } }.awaitAll()
    }