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

    private val dir: File get() = File(tmpFolder.root, "extensions_guard")

    @Test
    fun anUncaughtCrashQuarantinesTheExtensionsBeingLoaded() {
        ExtensionsGuard.markInFlight(dir, pkgName, pid = 1)
        ExtensionsGuard.markInFlight(dir, other, pid = 1)
        ExtensionsGuard.clearInFlight(dir, other)

        ExtensionsGuard.recordCrash(dir, inFlight = setOf(pkgName), startupCompleted = true)

        assertTrue(ExtensionsGuard.isQuarantined(dir, pkgName))
        assertFalse(ExtensionsGuard.isQuarantined(dir, other))
        // Dealt with: the next launch doesn't look at its exit reason
        ExtensionsGuard.checkLeftoverMarks(dir) { _, _ -> error("No mark left") }
    }

    @Test
    fun onlyANativeCrashOrAnAnrQuarantinesTheMarksLeft() {
        val reasons = mapOf(
            1 to ApplicationExitInfo.REASON_CRASH_NATIVE,
            2 to ApplicationExitInfo.REASON_ANR,
            3 to ApplicationExitInfo.REASON_USER_REQUESTED,
            4 to ApplicationExitInfo.REASON_LOW_MEMORY,
            5 to ApplicationExitInfo.REASON_SIGNALED,
            6 to ApplicationExitInfo.REASON_EXIT_SELF,
            // Caught by the uncaught exception handler, which quarantined already
            7 to ApplicationExitInfo.REASON_CRASH,
        )
        val pkgNames = (1..8).associateWith { "com.sf.tadami.extension.en.ext$it" }
        // 8: no exit reason, before Android 11 for instance
        pkgNames.forEach { (pid, pkgName) -> ExtensionsGuard.markInFlight(dir, pkgName, pid) }

        ExtensionsGuard.checkLeftoverMarks(dir) { pid, _ -> reasons[pid] }

        pkgNames.forEach { (pid, pkgName) ->
            assertEquals(pkgName, pid <= 2, ExtensionsGuard.isQuarantined(dir, pkgName))
        }
        // All cleared
        ExtensionsGuard.checkLeftoverMarks(dir) { _, _ -> error("No mark left") }
    }

    @Test
    fun twoCrashesInARowBeforeTheStartupCompletedTurnTheSafeModeOn() {
        ExtensionsGuard.recordCrash(dir, inFlight = emptySet(), startupCompleted = false)
        assertEquals(1, ExtensionsGuard.startupCrashes(dir))
        assertFalse(ExtensionsGuard.isSafeMode(dir))

        // After the startup: not counted
        ExtensionsGuard.recordCrash(dir, inFlight = emptySet(), startupCompleted = true)
        assertEquals(1, ExtensionsGuard.startupCrashes(dir))
        assertFalse(ExtensionsGuard.isSafeMode(dir))

        ExtensionsGuard.recordCrash(dir, inFlight = emptySet(), startupCompleted = false)
        assertEquals(ExtensionsGuard.SAFE_MODE_CRASHES, ExtensionsGuard.startupCrashes(dir))
        assertTrue(ExtensionsGuard.isSafeMode(dir))

        ExtensionsGuard.setSafeMode(dir, false)
        assertFalse(ExtensionsGuard.isSafeMode(dir))
    }

    @Test
    fun aCompletedStartupResetsTheCount() {
        ExtensionsGuard.recordCrash(dir, inFlight = emptySet(), startupCompleted = false)
        ExtensionsGuard.resetStartupCrashes(dir)
        assertEquals(0, ExtensionsGuard.startupCrashes(dir))

        ExtensionsGuard.recordCrash(dir, inFlight = emptySet(), startupCompleted = false)
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
}
