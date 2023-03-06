package com.sf.animescraper.network.requests.okhttp

import io.reactivex.rxjava3.core.Observable
import okhttp3.Call
import okhttp3.Response


fun Call.asObservable() : Observable<Response>{
    return Observable.fromCallable {
        this.execute().handleErrors()
    }
}

fun Response.handleErrors() : Response {
    val code = this.code
    if (!this.isSuccessful) {
        throw HttpError.Failure(code)
    }
    return this
}

