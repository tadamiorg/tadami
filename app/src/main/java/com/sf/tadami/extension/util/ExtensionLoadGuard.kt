package com.sf.tadami.extension.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Crash/hang barrier around third-party extension instantiation.
 *
 * [ExtensionsLoader] gives every extension a time budget, but a timeout only abandons the
 * *coroutine* — a constructor stuck in a blocking call keeps its thread forever, and one that
 * kills the process takes the whole app with it. Neither case is recoverable from within the
 * load that triggered it, so the fix has to survive into the next launch.
 *
 * Before touching a package's constructor we mark it "in flight" and flush that synchronously.
 * A clean load (success or a handled failure) clears the mark. Anything still marked when the
 * app next starts therefore hung or crashed us last time, and is skipped instead of retried.
 *
 * This deliberately uses [SharedPreferences] rather than DataStore: `commit()` is synchronous and
 * is on disk before we hand control to extension code, whereas a DataStore write is dispatched to
 * a coroutine that a wedged process may never get around to running.
 */
internal object ExtensionLoadGuard {

    private const val PREFS_NAME = "extension_load_state"
    private const val KEY_IN_FLIGHT = "in_flight"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Packages that were mid-load when the process last died. Read this once at the start of a
     * load pass, before [markInFlight] starts adding to the set again.
     */
    fun quarantined(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_IN_FLIGHT, emptySet()).orEmpty().toSet()

    fun markInFlight(context: Context, pkgName: String) {
        val p = prefs(context)
        synchronized(this) {
            val updated = p.getStringSet(KEY_IN_FLIGHT, emptySet()).orEmpty() + pkgName
            // commit(), not apply(): this has to be durable before the constructor runs.
            p.edit(commit = true) { putStringSet(KEY_IN_FLIGHT, updated) }
        }
    }

    fun clearInFlight(context: Context, pkgName: String) {
        val p = prefs(context)
        synchronized(this) {
            val updated = p.getStringSet(KEY_IN_FLIGHT, emptySet()).orEmpty() - pkgName
            p.edit(commit = true) { putStringSet(KEY_IN_FLIGHT, updated) }
        }
    }

    /** Lets the user retry a quarantined extension from the Extensions tab. */
    fun clearQuarantine(context: Context, pkgName: String) = clearInFlight(context, pkgName)

    fun clearAll(context: Context) {
        synchronized(this) {
            prefs(context).edit(commit = true) { remove(KEY_IN_FLIGHT) }
        }
    }
}
