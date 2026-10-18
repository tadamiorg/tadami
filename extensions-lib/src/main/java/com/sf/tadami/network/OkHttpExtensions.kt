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

fun OkHttpClient.shortTimeOutBuilder(timeOut : Long = 5) : OkHttpClient = throw Exception("Stub!")

fun Response.asJsoup(html: String? = null): Document = throw Exception("Stub!")


