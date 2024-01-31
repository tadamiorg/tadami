package com.sf.tadami.network

import com.sf.tadami.network.interceptors.UserAgentInterceptor
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
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.File
import java.io.OutputStream
import java.util.concurrent.TimeUnit
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
fun Call.asCancelableObservable(): Observable<Response> {

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
                    emitter.onNext(response)
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

fun OkHttpClient.Builder.setUserAgent(
    userAgent: String?,
    client : NetworkHelper
) = apply {
    interceptors().add(0, UserAgentInterceptor(userAgent ?: client.advancedPreferences.userAgent))
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

suspend fun Call.awaitSuccess(): Response {
    val callStack = Exception().stackTrace.run { copyOfRange(1, size) }
    val response = await(callStack)
    if (!response.isSuccessful) {
        response.close()
        throw HttpException(response.code).apply { stackTrace = callStack }
    }
    return response
}
class HttpException(val code: Int) : IllegalStateException("HTTP error $code")

context(Json)
inline fun <reified T> Response.parseAs(): T {
    return decodeFromString(serializer(),this.body.string())
}

context(Json)
inline fun <reified T> String.decodeOrNull(): T? {
    return try{
        decodeFromString(serializer(),this)
    }catch (e: Exception){
        null
    }
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

fun OkHttpClient.shortTimeOutBuilder(timeOut : Long = 5) : OkHttpClient{
    return this.newBuilder().callTimeout(timeOut, TimeUnit.SECONDS).build()
}

fun Response.asJsoup(html: String? = null): Document {
    return Jsoup.parse(html ?: body.string(), request.url.toString())
}


