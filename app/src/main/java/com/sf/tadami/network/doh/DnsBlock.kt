package com.sf.tadami.network.doh

import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * Whether a DNS blocking the host may have caused this error, so DNS over HTTPS could fix it:
 * no address, a block page answering with another certificate, or an address that refuses,
 * resets or never answers the connection.
 */
fun Throwable.mayBeDnsBlock(): Boolean = generateSequence(this) { it.cause }.any { error ->
    when (error) {
        is UnknownHostException, is SSLException, is SocketException -> true
        // Not a read timeout: the server answered, slowly
        is SocketTimeoutException -> error.message.orEmpty().contains("connect", ignoreCase = true)
        else -> false
    }
}
