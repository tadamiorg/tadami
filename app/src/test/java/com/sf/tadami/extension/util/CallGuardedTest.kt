package com.sf.tadami.extension.util

import com.sf.tadami.source.Source
import com.sf.tadami.utils.Lang
import io.reactivex.rxjava3.core.Observable
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.rx3.awaitSingle
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class CallGuardedTest {

    private class TestSource : Source {
        override val id = 1L
        override val name = "Test"
        override val lang = Lang.ENGLISH
    }

    private val source = TestSource()

    /** [e] and its causes: the coroutines copy an exception they rethrow, the copy caused by it */
    private fun causes(e: Throwable) = generateSequence(e) { it.cause }
    private val crashedSources = MutableStateFlow(emptyList<Source>())

    @Test
    fun returnsWhatTheSourceGives() = runBlocking {
        assertEquals("result", source.callGuarded(crashedSources) { "result" })
    }

    @Test
    fun runsOffTheCallingThread() = runBlocking {
        val caller = Thread.currentThread()
        val thread = source.callGuarded(crashedSources) { Thread.currentThread() }
        assertTrue(thread !== caller)
        assertTrue(thread.name, thread.name.startsWith("DefaultDispatcher-worker"))
    }

    @Test
    fun anErrorOfTheExtensionBecomesAnExtensionCodeException() = runBlocking {
        val error = NoSuchMethodError("setStatus")
        try {
            source.callGuarded(crashedSources) { throw error }
            fail("No exception")
        } catch (e: ExtensionCodeException) {
            assertTrue(causes(e).any { it === error })
        }
    }

    @Test
    fun anExceptionOfTheExtensionIsKept() = runBlocking {
        val exception = IllegalArgumentException("parse")
        try {
            source.callGuarded(crashedSources) { throw exception }
            fail("No exception")
        } catch (e: IllegalArgumentException) {
            assertTrue(causes(e).any { it === exception })
        }
    }

    @Test
    fun aCrashedSourceFailsTheCallWaitingForItAndDisposesItsFetch() = runBlocking {
        val disposed = CountDownLatch(1)
        val subscribed = CompletableDeferred<Unit>()
        val call = async(Dispatchers.Default) {
            runCatching {
                source.callGuarded(crashedSources) {
                    Observable.never<String>()
                        .doOnSubscribe { subscribed.complete(Unit) }
                        .doOnDispose { disposed.countDown() }
                        .awaitSingle()
                }
            }
        }
        withTimeout(5_000) { subscribed.await() }
        // Another source crashing changes nothing
        crashedSources.value = listOf(TestSource())
        crashedSources.value = crashedSources.value + source

        val error = withTimeout(5_000) { call.await() }.exceptionOrNull()
        assertTrue("$error", error is ExtensionDisabledException)
        assertEquals("Test", (error as ExtensionDisabledException).sourceName)
        assertTrue(disposed.await(5, TimeUnit.SECONDS))
    }

    @Test
    fun aSourceAlreadyCrashedFailsRightAway() = runBlocking {
        crashedSources.value = listOf(source)
        try {
            withTimeout(5_000) {
                source.callGuarded(crashedSources) { Observable.never<String>().awaitSingle() }
            }
            fail("No exception")
        } catch (e: ExtensionDisabledException) {
            assertEquals("Test", e.sourceName)
        }
    }

    @Test
    fun cancellingTheCallerDisposesTheFetch() = runBlocking {
        val disposed = CountDownLatch(1)
        val subscribed = CompletableDeferred<Unit>()
        val call = async(Dispatchers.Default) {
            source.callGuarded(crashedSources) {
                Observable.never<String>()
                    .doOnSubscribe { subscribed.complete(Unit) }
                    .doOnDispose { disposed.countDown() }
                    .awaitSingle()
            }
        }
        withTimeout(5_000) { subscribed.await() }
        call.cancel()
        assertTrue(disposed.await(5, TimeUnit.SECONDS))
    }
}
