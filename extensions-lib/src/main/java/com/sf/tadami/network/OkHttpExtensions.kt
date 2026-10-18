package com.sf.tadami.network

import io.reactivex.rxjava3.core.Observable
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Response
import org.jsoup.nodes.Document

fun Call.asObservable(): Observable<Response> = throw Exception("Stub!")
fun Call.asObservableSuccess(): Observable<Response> = throw Exception("Stub!")
fun Call.asCancelableObservable(): Observable<Response> = throw Exception("Stub!")

fun Response.handleErrors(): Response = throw Exception("Stub!")

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

fun OkHttpClient.shortTimeOutBuilder(timeOut : Long = 5) : OkHttpClient = throw Exception("Stub!")

fun Response.asJsoup(html: String? = null): Document = throw Exception("Stub!")


