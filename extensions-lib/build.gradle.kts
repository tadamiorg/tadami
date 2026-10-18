import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.android.library")
    kotlin("plugin.serialization")
}

android {
    namespace = "com.sf.tadami.extensions.lib"

    buildFeatures {
        buildConfig = false
        androidResources = false
    }
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        // Interface methods with a body become JVM default methods, plus DefaultImpls for the
        // extensions compiled without them. Pinned: the app uses the same mode (see :app).
        jvmDefault.set(JvmDefaultMode.ENABLE)
    }
}

// The app's versions: extensions compile against them (compileOnly) and get the app's at runtime
dependencies {
    api(libs.okhttp)
    api(libs.jsoup)
    api(libs.rxjava3)
    api(libs.injekt)
    api(kotlinx.serialization.json)
    api(platform(kotlinx.coroutines.bom))
    api(kotlinx.coroutines.core)
    api(androidx.datastore)
    api(platform(compose.bom))
    api(compose.ui)
    api(androidx.annotation)
}
