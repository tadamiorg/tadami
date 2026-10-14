package com.sf.tadami.extension.util

import android.app.ApplicationExitInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ExtensionsGuardTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val pkgName = "com.sf.tadami.extension.en.animepahe"
    private val other = "com.sf.tadami.extension.en.other"
    private val third = "com.sf.tadami.extension.fr.third"

    private val dir: File get() = File(tmpFolder.root, "extensions_guard")

    @Test
    fun aCrashQuarantinesOnlyTheExtensionsItComesFrom() {
        ExtensionsGuard.markInFlight(dir, pkgName, pid = 1)
        ExtensionsGuard.markInFlight(dir, other, pid = 1)

        ExtensionsGuard.recordCrash(dir, culprits = setOf(pkgName), inFlight = setOf(pkgName, other), startupCompleted = true)

        assertTrue(ExtensionsGuard.isQuarantined(dir, pkgName))
        // Loading at the same time, but the crash doesn't come from it
        assertFalse(ExtensionsGuard.isQuarantined(dir, other))
        assertFalse(ExtensionsGuard.isSuspect(dir, other))
        // Dealt with: the next launch doesn't look at their exit reason
        ExtensionsGuard.checkLeftoverMarks(dir) { _, _ -> error("No mark left") }
    }

    @Test
    fun aCrashFromNoExtensionMakesSuspectsOfTheOnesLoadingThenARepeatQuarantinesTheSuspect() {
        ExtensionsGuard.recordCrash(dir, culprits = emptySet(), inFlight = setOf(pkgName, other), startupCompleted = true)

        assertTrue(ExtensionsGuard.isSuspect(dir, pkgName))
        assertTrue(ExtensionsGuard.isSuspect(dir, other))
        assertFalse(ExtensionsGuard.isQuarantined(dir, pkgName))
        assertFalse(ExtensionsGuard.isQuarantined(dir, other))

        // The next launch loads each suspect alone: the repeat comes from the one loading
        ExtensionsGuard.recordCrash(dir, culprits = emptySet(), inFlight = setOf(pkgName), startupCompleted = false)

        assertTrue(ExtensionsGuard.isQuarantined(dir, pkgName))
        assertFalse(ExtensionsGuard.isSuspect(dir, pkgName))
        assertFalse(ExtensionsGuard.isQuarantined(dir, other))
        assertTrue(ExtensionsGuard.isSuspect(dir, other))
    }

    @Test
    fun aSuspectThatLoadedIsClearedAndACrashFromNothingLoadingChangesNoExtension() {
        ExtensionsGuard.recordCrash(dir, culprits = emptySet(), inFlight = setOf(pkgName), startupCompleted = true)
        ExtensionsGuard.clearSuspect(dir, pkgName)
        assertFalse(ExtensionsGuard.isSuspect(dir, pkgName))

        // Loaded fine: a later crash doesn't quarantine it
        ExtensionsGuard.recordCrash(dir, culprits = emptySet(), inFlight = setOf(pkgName), startupCompleted = true)
        assertTrue(ExtensionsGuard.isSuspect(dir, pkgName))
        assertFalse(ExtensionsGuard.isQuarantined(dir, pkgName))

        ExtensionsGuard.recordCrash(dir, culprits = emptySet(), inFlight = emptySet(), startupCompleted = true)
        assertTrue(ExtensionsGuard.isSuspect(dir, pkgName))
        assertFalse(ExtensionsGuard.isQuarantined(dir, pkgName))
    }

    @Test
    fun onlyANativeCrashOrAnAnrMakesSuspectsOfTheMarksLeft() {
        val reasons = mapOf(
            1 to ApplicationExitInfo.REASON_CRASH_NATIVE,
            2 to ApplicationExitInfo.REASON_ANR,
            3 to ApplicationExitInfo.REASON_USER_REQUESTED,
            4 to ApplicationExitInfo.REASON_LOW_MEMORY,
            5 to ApplicationExitInfo.REASON_SIGNALED,
            6 to ApplicationExitInfo.REASON_EXIT_SELF,
            // Caught by the uncaught exception handler, which dealt with it
            7 to ApplicationExitInfo.REASON_CRASH,
        )
        val pkgNames = (1..8).associateWith { "com.sf.tadami.extension.en.ext$it" }
        // 8: no exit reason, before Android 11 for instance
        pkgNames.forEach { (pid, pkgName) -> ExtensionsGuard.markInFlight(dir, pkgName, pid) }

        ExtensionsGuard.checkLeftoverMarks(dir) { pid, _ -> reasons[pid] }

        pkgNames.forEach { (pid, pkgName) ->
            assertEquals(pkgName, pid <= 2, ExtensionsGuard.isSuspect(dir, pkgName))
            assertFalse(pkgName, ExtensionsGuard.isQuarantined(dir, pkgName))
        }
        // All cleared
        ExtensionsGuard.checkLeftoverMarks(dir) { _, _ -> error("No mark left") }

        // A suspect loading alone when it crashes again in native code
        ExtensionsGuard.markInFlight(dir, pkgNames.getValue(1), pid = 9)
        ExtensionsGuard.checkLeftoverMarks(dir) { _, _ -> ApplicationExitInfo.REASON_CRASH_NATIVE }
        assertTrue(ExtensionsGuard.isQuarantined(dir, pkgNames.getValue(1)))
        assertFalse(ExtensionsGuard.isSuspect(dir, pkgNames.getValue(1)))
    }

    @Test
    fun twoCrashesInARowBeforeTheStartupCompletedTurnTheSafeModeOn() {
        ExtensionsGuard.recordCrash(dir, culprits = emptySet(), inFlight = emptySet(), startupCompleted = false)
        assertEquals(1, ExtensionsGuard.startupCrashes(dir))
        assertFalse(ExtensionsGuard.isSafeMode(dir))

        // After the startup: not counted
        ExtensionsGuard.recordCrash(dir, culprits = emptySet(), inFlight = emptySet(), startupCompleted = true)
        assertEquals(1, ExtensionsGuard.startupCrashes(dir))
        assertFalse(ExtensionsGuard.isSafeMode(dir))

        // Counted even when it comes from an extension
        ExtensionsGuard.recordCrash(dir, culprits = setOf(pkgName), inFlight = emptySet(), startupCompleted = false)
        assertEquals(ExtensionsGuard.SAFE_MODE_CRASHES, ExtensionsGuard.startupCrashes(dir))
        assertTrue(ExtensionsGuard.isSafeMode(dir))

        ExtensionsGuard.setSafeMode(dir, false)
        assertFalse(ExtensionsGuard.isSafeMode(dir))
    }

    @Test
    fun aCompletedStartupResetsTheCount() {
        ExtensionsGuard.recordCrash(dir, culprits = emptySet(), inFlight = emptySet(), startupCompleted = false)
        ExtensionsGuard.resetStartupCrashes(dir)
        assertEquals(0, ExtensionsGuard.startupCrashes(dir))

        ExtensionsGuard.recordCrash(dir, culprits = emptySet(), inFlight = emptySet(), startupCompleted = false)
        assertEquals(1, ExtensionsGuard.startupCrashes(dir))
        assertFalse(ExtensionsGuard.isSafeMode(dir))

        // The crash screen turns it on without any count
        ExtensionsGuard.setSafeMode(dir, true)
        assertTrue(ExtensionsGuard.isSafeMode(dir))
        ExtensionsGuard.setSafeMode(dir, false)
        assertFalse(ExtensionsGuard.isSafeMode(dir))
        ExtensionsGuard.setSafeMode(dir, true)
        assertTrue(ExtensionsGuard.isSafeMode(dir))
    }

    // Attribution: which extension code defines each class, the others being the app's

    private val sharedLib = "com.sf.tadami.lib.extractors.Extractor"

    private val definedClasses = mapOf(
        pkgName to setOf("$pkgName.AnimePahe", "$pkgName.AnimePahe\$init\$1", sharedLib),
        other to setOf("$other.Other", sharedLib),
        third to setOf("$third.Third"),
    )

    private fun attribute(throwable: Throwable, inFlight: Set<String> = emptySet()) =
        ExtensionsGuard.attribute(throwable, definedClasses.keys, inFlight) { pkgName, className ->
            className in definedClasses.getValue(pkgName)
        }

    private fun throwable(vararg classNames: String, cause: Throwable? = null) =
        RuntimeException("crash", cause).apply {
            stackTrace = classNames.map { StackTraceElement(it, "run", null, 1) }.toTypedArray()
        }

    private val appFrames = arrayOf(
        "com.sf.tadami.source.online.AnimeHttpSource",
        "io.reactivex.rxjava3.internal.operators.observable.ObservableMap\$MapObserver",
        "java.lang.Thread",
    )

    @Test
    fun aCrashComesFromTheExtensionsDefiningAClassOfItsFrames() {
        val crash = throwable("$pkgName.AnimePahe\$init\$1", *appFrames)
        // Even with others loading at the same time
        assertEquals(setOf(pkgName), attribute(crash, inFlight = setOf(other, third)))
    }

    @Test
    fun aCrashOfTheAppCodeOnlyComesFromNoExtension() {
        assertEquals(emptySet<String>(), attribute(throwable(*appFrames), inFlight = setOf(pkgName)))
    }

    @Test
    fun theFramesOfTheCausesAndSuppressedExceptionsCount() {
        val fromCause = throwable(*appFrames, cause = throwable("$third.Third"))
        assertEquals(setOf(third), attribute(fromCause))

        val fromSuppressed = throwable(*appFrames).apply { addSuppressed(throwable("$other.Other")) }
        assertEquals(setOf(other), attribute(fromSuppressed))

        // A cycle of causes ends
        val first = RuntimeException("first")
        val second = RuntimeException("second", first)
        first.initCause(second)
        first.stackTrace = arrayOf(StackTraceElement("$pkgName.AnimePahe", "run", null, 1))
        assertEquals(setOf(pkgName), attribute(second))
    }

    @Test
    fun theClassesOfItsOwnPackageWinOverThoseOfALibraryOthersBundle() {
        val crash = throwable(sharedLib, "$other.Other", *appFrames)
        assertEquals(setOf(other), attribute(crash, inFlight = setOf(pkgName)))
    }

    @Test
    fun aSharedLibraryCrashComesFromTheExtensionsLoadingElseFromAllThatBundleIt() {
        val crash = throwable(sharedLib, *appFrames)
        assertEquals(setOf(pkgName), attribute(crash, inFlight = setOf(pkgName, third)))
        assertEquals(setOf(pkgName, other), attribute(crash, inFlight = setOf(third)))
        assertEquals(setOf(pkgName, other), attribute(crash))
    }
}
