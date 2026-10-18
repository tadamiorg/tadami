package com.sf.tadami.extension.util

import com.sf.tadami.extension.ExtensionManager
import com.sf.tadami.source.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Calls the code of the extension of this source, on [Dispatchers.IO]. RxJava throws a fatal error,
 * a LinkageError for instance, up the thread emitting it, instead of giving it to the subscriber,
 * which would wait forever:
 * - an error the call throws, while subscribing to the Rx fetch for instance, fails it as an
 *   [ExtensionCodeException];
 * - an error thrown on another thread ends that thread, which unloads the extension, see
 *   [ExtensionManager.disableCrashedExtensions]: the call fails with an [ExtensionDisabledException].
 *
 * Cancelling the caller cancels the call, and disposes the Rx fetch the suspend method waits for.
 */
suspend fun <S : Source, T> S.callGuarded(block: suspend S.() -> T): T =
    callGuarded(Injekt.get<ExtensionManager>().crashedSourcesFlow, block)

/** [callGuarded], the call failing once [crashedSources] has this source. */
internal suspend fun <S : Source, T> S.callGuarded(crashedSources: Flow<List<Source>>, block: suspend S.() -> T): T {
    val source = this
    return coroutineScope {
        // Fails the scope, so cancels the call, once the source is unloaded
        val disabled = launch {
            crashedSources.first { sources -> sources.any { it === source } }
            throw ExtensionDisabledException(source.name)
        }
        try {
            withContext(Dispatchers.IO) {
                try {
                    source.block()
                } catch (e: Throwable) {
                    throw e as? Exception ?: ExtensionCodeException(e)
                }
            }
        } finally {
            disabled.cancel()
        }
    }
}

/** An error, not an exception, thrown by the code of an extension, see [callGuarded]. */
class ExtensionCodeException(cause: Throwable) : Exception(cause.toString(), cause)

/** The extension of the source [sourceName] crashed, and was unloaded, see [callGuarded]. */
class ExtensionDisabledException(val sourceName: String) : Exception("Disabled after a crash: $sourceName")
