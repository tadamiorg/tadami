package com.sf.tadami.network

import io.reactivex.rxjava3.core.Observable
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Response
import org.jsoup.nodes.Document

/**
 * Runs the call, blocking, on subscription, and fails with [HttpException] when the response isn't
 * a success (2xx). Disposing cancels it.
 *
 * @since extensions-lib 1.5
 */
fun Call.asObservable(): Observable<Response> = throw Exception("Stub!")

/**
 * Runs the call, blocking, on subscription, and gives the response whatever its code. Disposing
 * cancels it.
 *
 * @since extensions-lib 1.5
 */
fun Call.asObservableSuccess(): Observable<Response> = throw Exception("Stub!")

/**
 * Runs the call asynchronously, on OkHttp's threads, and fails with [HttpException] when the
 * response isn't a success (2xx). Disposing cancels it.
 *
 * @since extensions-lib 1.5
 */
fun Call.asCancelableObservable(): Observable<Response> = throw Exception("Stub!")

/**
 * This response, or [HttpException] when it isn't a success (2xx).
 *
 * @since extensions-lib 1.5
 */
fun Response.handleErrors(): Response = throw Exception("Stub!")

/**
 * Runs the call without blocking the thread, and cancels it when the coroutine is cancelled. Gives
 * the response whatever its code: see [awaitSuccess].
 *
 * @since extensions-lib 1.5
 */
suspend fun Call.await(): Response = throw Exception("Stub!")

/**
 * Runs the call without blocking the thread, and cancels it when the coroutine is cancelled.
 *
 * @throws HttpException when the response isn't a success (2xx), closed then
 * @since extensions-lib 1.7
 */
suspend fun Call.awaitSuccess(): Response = throw Exception("Stub!")

/**
 * A response that isn't a success, thrown by [awaitSuccess] and [handleErrors].
 *
 * @property code its HTTP status code
 * @since extensions-lib 1.7
 */
class HttpException(val code: Int) : IllegalStateException("HTTP error $code")

/**
 * A copy of this client whose calls time out after [timeOut] seconds.
 *
 * @since extensions-lib 1.5
 */
fun OkHttpClient.shortTimeOutBuilder(timeOut : Long = 5) : OkHttpClient = throw Exception("Stub!")

/**
 * The body of this response, or [html], parsed by Jsoup, relative to the URL of the request.
 *
 * @since extensions-lib 1.5
 */
fun Response.asJsoup(html: String? = null): Document = throw Exception("Stub!")
