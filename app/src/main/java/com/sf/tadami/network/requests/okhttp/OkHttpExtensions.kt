package com.sf.tadami.network.requests.okhttp

import com.sf.tadami.notifications.utils.okhttp.ProgressListener
import com.sf.tadami.notifications.utils.okhttp.ProgressResponseBody
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import okhttp3.*
import okhttp3.Callback
import okio.BufferedSource
import okio.IOException
import okio.buffer
import okio.sink
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.io.OutputStream
import kotlin.coroutines.resumeWithException

fun Call.asObservable(): Observable<Response> {
    return Observable.fromCallable {
        this.execute().handleErrors()
    }
}

fun Call.asObservableSuccess(): Observable<Response> {
    return Observable.fromCallable {
        this.execute()
    }
}
fun <T : Any>Call.asCancelableObservable(mapper : (response : Response) -> T): Observable<T> {

    return Observable.create { emitter ->
        val callback = object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if(!call.isCanceled()){
                    emitter.onError(e)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    response.handleErrors()
                    emitter.onNext(mapper(response))
                    emitter.onComplete()
                } catch (e: Exception) {
                    if(!call.isCanceled()) {
                        emitter.onError(e)
                    }
                }
            }
        }
        this.enqueue(callback)

        emitter.setDisposable(object : Disposable {
            private var disposed = false
            override fun dispose() {
                disposed = true
                this@asCancelableObservable.cancel()
            }

            override fun isDisposed(): Boolean = disposed
        })
    }
}

fun Response.handleErrors(): Response {
    val code = this.code
    if (!this.isSuccessful) {
        throw HttpError.Failure(code)
    }
    return this
}

fun OkHttpClient.newCachelessCallWithProgress(request: Request, listener: ProgressListener): Call {
    val progressClient = newBuilder()
        .cache(null)
        .addNetworkInterceptor { chain ->
            val originalResponse = chain.proceed(chain.request())
            originalResponse.newBuilder()
                .body(ProgressResponseBody(originalResponse.body, listener))
                .build()
        }
        .build()

    return progressClient.newCall(request)
}

@OptIn(ExperimentalCoroutinesApi::class)
private suspend fun Call.await(callStack: Array<StackTraceElement>): Response {
    return suspendCancellableCoroutine { continuation ->
        val callback =
            object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    continuation.resume(response) {
                        response.body.close()
                    }
                }

                override fun onFailure(call: Call, e: java.io.IOException) {
                    // Don't bother with resuming the continuation if it is already cancelled.
                    if (continuation.isCancelled) return
                    val exception = java.io.IOException(e.message, e).apply { stackTrace = callStack }
                    continuation.resumeWithException(exception)
                }
            }

        enqueue(callback)

        continuation.invokeOnCancellation {
            try {
                cancel()
            } catch (ex: Throwable) {
                // Ignore cancel exception
            }
        }
    }
}

suspend fun Call.await(): Response {
    val callStack = Exception().stackTrace.run { copyOfRange(1, size) }
    return await(callStack)
}

inline fun <reified T> Response.parseAs(): T {
    return Injekt.get<Json>().decodeFromString(serializer(),this.body.string())
}

fun BufferedSource.saveTo(file: File) {
    try {
        // Create parent dirs if needed
        file.parentFile?.mkdirs()

        // Copy to destination
        saveTo(file.outputStream())
    } catch (e: Exception) {
        close()
        file.delete()
        throw e
    }
}

fun BufferedSource.saveTo(stream: OutputStream) {
    use { input ->
        stream.sink().buffer().use {
            it.writeAll(input)
            it.flush()
        }
    }
}


