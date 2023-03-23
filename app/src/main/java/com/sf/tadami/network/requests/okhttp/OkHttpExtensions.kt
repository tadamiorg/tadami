package com.sf.tadami.network.requests.okhttp

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import okhttp3.Call
import okhttp3.Response
import okio.IOException

fun Call.asObservable(): Observable<Response> {
    return Observable.fromCallable {
        this.execute().handleErrors()
    }
}
fun <T : Any>Call.asCancelableObservable(mapper : (response : Response) -> T): Observable<T> {

    return Observable.create { emitter ->
        val callback = object : okhttp3.Callback {
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

