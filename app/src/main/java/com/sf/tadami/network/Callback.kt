package com.sf.tadami.network

interface Callback<T> {
    fun onData(data: T?){

    }
    fun onError(message : String?, errorCode : Int? = null){

    }
}