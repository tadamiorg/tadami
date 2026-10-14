package com.sf.tadami.extension.util

import com.sf.tadami.extension.ExtensionManager
import com.sf.tadami.source.Source
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.rx3.asObservable
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

/** An error, not an exception, thrown by the code of an extension, see [fetchGuarded]. */
class ExtensionCodeException(cause: Throwable) : Exception(cause.toString(), cause)

/** The extension of the source [sourceName] crashed, and was unloaded, see [fetchGuarded]. */
class ExtensionDisabledException(val sourceName: String) : Exception("Disabled after a crash: $sourceName")
