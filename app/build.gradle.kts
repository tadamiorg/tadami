plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlinx-serialization")
    id("app.cash.sqldelight") version "2.0.0-alpha05"
}

sqldelight {
    databases {
        create("Database") {
            packageName.set("com.sf.tadami")
            dialect("app.cash.sqldelight:sqlite-3-24-dialect:2.0.0-alpha05")
        }
    }
}

android {
    namespace = "com.sf.tadami"

    defaultConfig {
        applicationId = "com.sf.tadami"
        versionCode = 2
        versionName = "1.0.1"

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
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        named("release") {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.4"
    }
    packagingOptions {
        resources {
            excludes.add("/META-INF/{AL2.0,LGPL2.1}")
        }
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.9.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // Kotlin Coroutines

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-rx3:1.6.4")
    implementation("com.google.android.material:material:1.8.0")

    // Coil

    val coilVersion = "2.2.2"

    implementation("io.coil-kt:coil:$coilVersion")
    implementation("io.coil-kt:coil-compose:$coilVersion")

    // Exoplayer Dependencies

    val exoplayerVersion = "2.18.5"

    implementation("com.google.android.exoplayer:exoplayer:$exoplayerVersion")
    implementation("com.google.android.exoplayer:extension-cast:$exoplayerVersion")
    implementation("com.google.android.exoplayer:exoplayer-dash:$exoplayerVersion")
    implementation("com.google.android.exoplayer:exoplayer-hls:$exoplayerVersion")
    implementation("com.google.android.exoplayer:exoplayer-smoothstreaming:$exoplayerVersion")
    implementation("com.google.android.exoplayer:exoplayer-rtsp:$exoplayerVersion")

    // Compose dependencies

    val navVersion = "2.5.3"
    val composeVersion = "1.3.3"

    // Compose nav dependencies

    implementation("androidx.activity:activity-compose:1.7.0")
    implementation("androidx.compose.animation:animation:$composeVersion")
    implementation("androidx.compose.runtime:runtime:$composeVersion")
    implementation("androidx.compose.runtime:runtime-saveable:$composeVersion")
    implementation("androidx.compose.ui:ui:$composeVersion")
    implementation("androidx.navigation:navigation-runtime-ktx:$navVersion")
    implementation("androidx.navigation:navigation-compose:$navVersion")

    // Compose Material

    implementation("androidx.compose.material3:material3:1.0.1")
    implementation("androidx.compose.material:material:1.3.1")
    implementation("androidx.compose.material:material-icons-extended:1.3.1")

    // Other compose dependencies
    val composeAccompanist = "0.30.0"

    implementation("androidx.compose.foundation:foundation:1.4.0")

    implementation("com.google.accompanist:accompanist-themeadapter-material3:$composeAccompanist")
    implementation("com.google.accompanist:accompanist-systemuicontroller:$composeAccompanist")
    implementation("com.google.accompanist:accompanist-flowlayout:$composeAccompanist")

    implementation("androidx.compose.ui:ui-tooling-preview:$composeVersion")
    implementation("androidx.compose.ui:ui-util:$composeVersion")

    androidTestImplementation("androidx.compose.ui:ui-test-junit4:$composeVersion")
    debugImplementation("androidx.compose.ui:ui-tooling:$composeVersion")
    debugImplementation("androidx.compose.ui:ui-test-manifest:$composeVersion")

    // ViewModel Dependencies

    val lifecycleVersion = "2.6.0"
    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion")
    // ViewModel utilities for Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycleVersion")
    // LiveData
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycleVersion")
    // Saved state module for ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-savedstate:$lifecycleVersion")

    // RX

    implementation("io.reactivex.rxjava3:rxandroid:3.0.0")
    implementation("io.reactivex.rxjava3:rxjava:3.0.2")

    // JSoup

    implementation("org.jsoup:jsoup:1.15.3")
    implementation("androidx.webkit:webkit:1.6.1")

    // Injekt Dependency injection

    implementation("com.github.inorichi.injekt:injekt-core:65b0440")

    // Json Serialization

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")

    // Network client okhttp
    val okhttpVersion = "5.0.0-alpha.11"
    implementation("com.squareup.okhttp3:okhttp:$okhttpVersion")
    implementation("com.squareup.okhttp3:logging-interceptor:$okhttpVersion")
    implementation("com.squareup.okhttp3:okhttp-dnsoverhttps:$okhttpVersion")
    implementation("com.squareup.okio:okio:3.2.0")

    // SQLDelight

    implementation("app.cash.sqldelight:android-driver:2.0.0-alpha05")
    implementation("app.cash.sqldelight:coroutines-extensions:2.0.0-alpha05")
    implementation("com.github.requery:sqlite-android:3.39.2")

    // Paging Library 3

    val pagingVersion = "3.1.1"

    implementation("androidx.paging:paging-runtime:$pagingVersion")
    // alternatively - without Android dependencies for tests
    testImplementation("androidx.paging:paging-common:$pagingVersion")
    // optional - RxJava3 support
    implementation("androidx.paging:paging-rxjava3:$pagingVersion")
    // optional - Jetpack Compose integration
    implementation("androidx.paging:paging-compose:1.0.0-alpha18")

    // Preference data store

    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Wheel Picker

    implementation("com.github.commandiron:WheelPickerCompose:1.1.10")

    // Worker

    val workVersion = "2.8.1"

    implementation("androidx.work:work-runtime-ktx:$workVersion")

    // SplashScreen

    implementation("androidx.core:core-splashscreen:1.0.0-beta02")

    // Rich Text for Markdown mostly

    val richtextVersion = "0.16.0"

    implementation("com.halilibo.compose-richtext:richtext-ui-material3:${richtextVersion}")
    implementation("com.halilibo.compose-richtext:richtext-commonmark:${richtextVersion}")


}