plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    kotlin("plugin.serialization")
    id("app.cash.sqldelight")
}

sqldelight {
    databases {
        create("Database") {
            packageName.set("com.sf.tadami")
            dialect(libs.sqldelight.dialects.sql)
        }
    }
}

android {
    namespace = "com.sf.tadami"

    defaultConfig {
        applicationId = "com.sf.tadami"
        versionCode = 16
        versionName = "1.2.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        named("debug"){
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
            isDebuggable = true
            isShrinkResources = true
            isMinifyEnabled = true
            proguardFiles("proguard-android-optimize.txt", "proguard-rules.pro")
        }
        named("release") {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles("proguard-android-optimize.txt", "proguard-rules.pro")
        }
    }

    buildFeatures {
        compose = true
        viewBinding = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = compose.versions.compiler.get()
    }
    packagingOptions {
        resources {
            excludes.add("/META-INF/{AL2.0,LGPL2.1}")
        }
    }
}

dependencies {

    implementation(androidx.core.ktx)

    testImplementation(androidx.junit)
    androidTestImplementation(androidx.androidx.test.ext.junit)
    androidTestImplementation(androidx.espresso.core)

    // Kotlin Coroutines
    implementation(platform(kotlinx.coroutines.bom))
    implementation(kotlinx.bundles.coroutines)
    implementation(androidx.material)

    // Coil
    implementation(platform(libs.coil.bom))
    implementation(libs.bundles.coil)

    // Exoplayer
    implementation(libs.bundles.exoplayer)

    // Compose
    implementation(platform(compose.bom))
    implementation(compose.activity)
    implementation(compose.bundles.animation)
    implementation(compose.bundles.ui)
    implementation(compose.bundles.runtime)
    implementation(compose.bundles.material)
    implementation(compose.bundles.accompanist)
    implementation(androidx.bundles.navigation)

    // ViewModel Dependencies
    implementation(androidx.bundles.lifecycle)

    // RXJAVA
    implementation(libs.bundles.rxjava)

    // JSoup
    implementation(libs.jsoup)
    implementation(androidx.webkit)

    // Injekt Dependency injection
    implementation(libs.injekt)

    // Json Serialization
    implementation(kotlinx.bundles.serialization)

    // Network client okhttp
    implementation(libs.bundles.okhttp)

    // SQLDelight
    implementation(libs.bundles.sqldelight)

    // Paging Library 3
    implementation(androidx.bundles.paging)

    // Preference data store
    implementation(androidx.datastore)

    // Wheel Picker
    implementation(libs.wheel.picker)

    // Worker
    implementation(androidx.worker)

    // SplashScreen
    implementation(androidx.splashscreen)

    // Mardown parser
    implementation(libs.mardown.parser)

    // Cast Dependencies
    implementation(androidx.appcompat)
    implementation(androidx.bundles.cast)

    // Http4k proxy to cast streams
    implementation(platform(libs.http4k.bom))
    implementation(libs.bundles.http4k)

    // JSUnpacker
    implementation(libs.jsunpacker)

}
tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.freeCompilerArgs += listOf(
            "-Xcontext-receivers"
        )
    }
}