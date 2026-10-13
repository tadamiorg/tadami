package com.sf.tadami.network

import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.plugins.RxJavaPlugins
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/** Disposing the observable of a blocking call must cancel it, and never crash through RxJavaPlugins. */
class OkHttpExtensionsTest {

    private lateinit var server: MockWebServer
    private val slowStarted = CountDownLatch(1)
    private val releaseSlow = CountDownLatch(1)
    private val undeliverable = Collections.synchronizedList(mutableListOf<Throwable>())
    private val client = OkHttpClient.Builder().readTimeout(10, TimeUnit.SECONDS).build()

    @Before
    fun setUp() {
        RxJavaPlugins.setErrorHandler { undeliverable.add(it) }
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when (request.url.encodedPath) {
                    "/slow" -> {
                        slowStarted.countDown()
                        releaseSlow.await(10, TimeUnit.SECONDS)
                        MockResponse.Builder().code(500).build()
                    }
                    "/large" -> MockResponse.Builder().body("a".repeat(LARGE_BODY_LENGTH)).build()
                    else -> MockResponse.Builder().code(500).build()
                }
            }
        }
        server.start()
    }

    @After
    fun tearDown() {
        releaseSlow.countDown()
        server.close()
        RxJavaPlugins.reset()
    }

    @Test
    fun disposingCancelsTheCallWithoutAnUndeliverableError() {
        val call = client.newCall(request("/slow"))
        val subscribed = CountDownLatch(1)
        var disposable: Disposable? = null
        val subscriber = thread {
            call.asObservable().subscribe(object : Observer<Response> {
                override fun onSubscribe(d: Disposable) {
                    disposable = d
                    subscribed.countDown()
                }

                override fun onNext(t: Response) = Unit
                override fun onError(e: Throwable) = Unit
                override fun onComplete() = Unit
            })
        }
        assertTrue(subscribed.await(5, TimeUnit.SECONDS))
        assertTrue(slowStarted.await(5, TimeUnit.SECONDS))

        disposable!!.dispose()
        subscriber.join(5000)

        assertTrue(call.isCanceled())
        assertFalse("The blocking call kept running after the dispose", subscriber.isAlive)
        assertTrue(undeliverable.toString(), undeliverable.isEmpty())
    }

    @Test
    fun theBodyStaysReadableOnceTheResponseIsDelivered() {
        val response = client.newCall(request("/large")).asObservableSuccess().blockingFirst()
        response.use { assertEquals(LARGE_BODY_LENGTH, it.body.string().length) }
    }

    @Test
    fun subscribersStillReceiveHttpErrors() {
        val error = runCatching {
            client.newCall(request("/error")).asObservable().blockingFirst()
        }.exceptionOrNull()
        assertEquals(500, (error as HttpException).code)
    }

    private fun request(path: String) = Request.Builder().url(server.url(path)).build()

    private companion object {
        const val LARGE_BODY_LENGTH = 4 * 1024 * 1024
    }
}
