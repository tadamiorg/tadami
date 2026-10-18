package com.sf.tadami.extension.util

import com.sf.tadami.extension.ExtensionManager
import com.sf.tadami.source.Source
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.asObservable
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Fetches with the code of the extension of this source. RxJava throws a fatal error, a
 * LinkageError for instance, up the thread emitting it, instead of giving it to the subscriber,
 * which would wait forever:
 * - thrown while subscribing, it fails the Observable as an [ExtensionCodeException];
 * - thrown on another thread, it ends that thread, which unloads the extension, see
 *   [ExtensionManager.disableCrashedExtensions]: the Observable fails with an
 *   [ExtensionDisabledException].
 */
fun <S : Source, T : Any> S.fetchGuarded(fetch: S.() -> Observable<T>): Observable<T> {
    val source = this
    val fetching = Observable.create { emitter ->
        try {
            source.fetch().subscribe(object : Observer<T> {
                override fun onSubscribe(d: Disposable) = emitter.setDisposable(d)
                override fun onNext(t: T) = emitter.onNext(t)
                override fun onError(e: Throwable) {
                    emitter.tryOnError(e as? Exception ?: ExtensionCodeException(e))
                }
                override fun onComplete() = emitter.onComplete()
            })
        } catch (e: Throwable) {
            emitter.tryOnError(e as? Exception ?: ExtensionCodeException(e))
        }
    }
    val disabled = Injekt.get<ExtensionManager>().crashedSourcesFlow
        .filter { sources -> sources.any { it === source } }
        .take(1)
        .asObservable()
        .flatMap { Observable.error<T>(ExtensionDisabledException(source.name)) }
    // The first to answer
    return Observable.ambArray(fetching, disabled)
}

/**
 * Calls the code of the extension of this source, on [Dispatchers.IO], like [fetchGuarded]:
 * - an error it throws, a LinkageError for instance, fails the call as an [ExtensionCodeException];
 * - an error it throws on another thread ends that thread, which unloads the extension, see
 *   [ExtensionManager.disableCrashedExtensions]: the call, which would wait forever, fails with an
 *   [ExtensionDisabledException].
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
