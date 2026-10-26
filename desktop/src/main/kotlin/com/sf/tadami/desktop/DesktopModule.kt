package com.sf.tadami.desktop

import com.sf.tadami.desktop.extension.ExtensionManager
import com.sf.tadami.network.NetworkHelper
import dev.mihon.injekt.patchInjekt
import kotlinx.serialization.json.Json
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektModule
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.addSingleton
import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.get

/** The singletons of the desktop app, and those the extensions get from Injekt. */
class DesktopModule : InjektModule {

    override fun InjektRegistrar.registerInjectables() {
        addSingleton(
            Json {
                ignoreUnknownKeys = true
                explicitNulls = false
            },
        )
        addSingletonFactory { NetworkHelper() }
        addSingletonFactory { ExtensionManager(get(), get()) }
    }

    companion object {
        fun install() {
            patchInjekt()
            Injekt.importModule(DesktopModule())
        }
    }
}
