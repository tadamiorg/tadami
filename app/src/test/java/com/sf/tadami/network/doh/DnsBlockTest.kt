package com.sf.tadami.network.doh

import com.sf.tadami.network.HttpException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLPeerUnverifiedException

class DnsBlockTest {

    @Test
    fun anUnresolvedHostMayBeBlocked() {
        assertTrue(UnknownHostException("Unable to resolve host \"example.com\"").mayBeDnsBlock())
    }

    @Test
    fun aCertificateForAnotherHostMayBeABlockPage() {
        assertTrue(SSLPeerUnverifiedException("Hostname example.com not verified").mayBeDnsBlock())
        assertTrue(SSLHandshakeException("Chain validation failed").mayBeDnsBlock())
    }

    @Test
    fun aRefusedOrResetConnectionMayBeBlocked() {
        assertTrue(ConnectException("Failed to connect to example.com/0.0.0.0:443").mayBeDnsBlock())
        assertTrue(SocketException("Connection reset").mayBeDnsBlock())
    }

    @Test
    fun aConnectTimeoutMayBeBlocked() {
        // Android's message, then the JVM's
        assertTrue(
            SocketTimeoutException("failed to connect to example.com/10.0.0.1 (port 443) from /192.168.1.2 (port 50000) after 30000ms")
                .mayBeDnsBlock(),
        )
        assertTrue(SocketTimeoutException("Connect timed out").mayBeDnsBlock())
    }

    @Test
    fun aReadTimeoutIsASlowServerNotABlock() {
        assertFalse(SocketTimeoutException("timeout").mayBeDnsBlock())
        assertFalse(SocketTimeoutException("Read timed out").mayBeDnsBlock())
    }

    @Test
    fun anHttpErrorCameFromTheServer() {
        assertFalse(HttpException(403).mayBeDnsBlock())
        assertFalse(HttpException(503).mayBeDnsBlock())
    }

    @Test
    fun anOtherErrorIsNotABlock() {
        assertFalse(IllegalStateException("No episodes").mayBeDnsBlock())
        assertFalse(IOException("unexpected end of stream").mayBeDnsBlock())
    }

    @Test
    fun aWrappedNetworkErrorCounts() {
        assertTrue(IOException(UnknownHostException("example.com")).mayBeDnsBlock())
    }
}
