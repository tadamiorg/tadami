import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    kotlin("plugin.serialization")
    id("app.cash.sqldelight")
    alias(kotlinx.plugins.compose.compiler)
}

sqldelight {
    databases {
        create("Database") {
            packageName.set("com.sf.tadami")
            dialect(libs.sqldelight.dialects.sql)
            schemaOutputDirectory.set(file("src/main/sqldelight/databases"))
        }
    }
}

android {
    namespace = "com.sf.tadami"
    android.buildFeatures.buildConfig=true

    defaultConfig {
        applicationId = "com.sf.tadami"
        versionCode = 36
        versionName = "1.6.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "BUILD_DATE", "\"${getBuildDate()}\"")
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release"){
            storeFile = file(env.KEY_FILE.value)
            storePassword = env.KEY_STORE_PASSWORD.value
            keyAlias = env.ALIAS.value
            keyPassword = env.KEY_STORE_PASSWORD.value
        }
    }

    buildTypes {
        named("debug"){
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
            isDebuggable = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles("proguard-android-optimize.txt", "proguard-rules.pro")
        }
        named("release") {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles("proguard-android-optimize.txt", "proguard-rules.pro")
        }
    }
    buildFeatures {
        compose = true
        viewBinding = true
    }
    packaging {
        resources {
            excludes.add("/META-INF/{AL2.0,LGPL2.1}")
        }
    }
    sourceSets {
        getByName("main") {
            res {
                srcDirs("src\\main\\res", "src\\main\\res\\chromecast-res")
            }
        }
    }
}

fun getBuildDate(): String {
    val date = Date()
    val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
    format.timeZone = TimeZone.getTimeZone("UTC")
    return format.format(date)
}

dependencies {

    implementation(androidx.core.ktx)


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
    implementation(androidx.bundles.exoplayer)

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

    // Markdown parser
    implementation(libs.bundles.richtext)

    // Cast Dependencies
    implementation(androidx.appcompat)
    implementation(androidx.bundles.cast)

    // Http4k proxy to cast streams
    implementation(platform(libs.http4k.bom))
    implementation(libs.bundles.http4k)

    // JSUnpacker
    implementation(libs.jsunpacker)
    implementation(libs.unifile)

}
tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions {
            freeCompilerArgs = listOf(
                "-Xcontext-receivers"
            )
        }
    }
}