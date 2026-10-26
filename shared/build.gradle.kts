import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    kotlin("multiplatform")
    id("com.android.kotlin.multiplatform.library")
    alias(kotlinx.plugins.compose.compiler)
    alias(kotlinx.plugins.compose.multiplatform)
    kotlin("plugin.serialization")
    id("app.cash.sqldelight")
}

kotlin {
    android {
        namespace = "com.sf.tadami.shared"
        compileSdk = 37
        minSdk = 28
        androidResources {
            enable = true
        }
    }
    jvm("desktop")
    jvmToolchain(17)

    compilerOptions {
        freeCompilerArgs.addAll("-Xexpect-actual-classes", "-Xcontext-parameters", "-XXLanguage:+PropertyParamAnnotationDefaultTargetMode")
        optIn.addAll(
            "androidx.compose.material3.ExperimentalMaterial3Api",
            "androidx.compose.foundation.ExperimentalFoundationApi",
            "androidx.compose.animation.ExperimentalAnimationApi",
            "kotlinx.coroutines.ExperimentalCoroutinesApi",
            "kotlinx.serialization.ExperimentalSerializationApi",
        )
    }

    sourceSets {
        commonMain.dependencies {
            // UI
            api("org.jetbrains.compose.runtime:runtime:1.12.2")
            api("org.jetbrains.compose.runtime:runtime-saveable:1.12.2")
            api("org.jetbrains.compose.foundation:foundation:1.12.2")
            api("org.jetbrains.compose.animation:animation:1.12.2")
            api("org.jetbrains.compose.animation:animation-graphics:1.12.2")
            api("org.jetbrains.compose.ui:ui:1.12.2")
            api("org.jetbrains.compose.ui:ui-util:1.12.2")
            api("org.jetbrains.compose.material:material:1.12.2")
            api("org.jetbrains.compose.material3:material3:1.9.0")
            api("org.jetbrains.compose.material:material-icons-extended:1.7.3")
            api("org.jetbrains.compose.components:components-resources:1.12.2")
            api("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
            api("org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
            api("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-savedstate:2.11.0")
            api("org.jetbrains.androidx.navigation:navigation-compose:2.10.0")
            api("androidx.paging:paging-common:3.5.3")
            api("androidx.paging:paging-compose:3.5.3")
            api(composeLibs.compose.materialmotion)
            api(libs.bundles.richtext)
            api("io.coil-kt.coil3:coil-compose:3.6.3")
            api("io.coil-kt.coil3:coil-network-okhttp:3.6.3")

            // Data
            api(project.dependencies.platform(kotlinx.coroutines.bom))
            api(kotlinx.coroutines.core)
            api(kotlinx.coroutines.rx3)
            api(kotlinx.serialization.json)
            api(kotlinx.serialization.json.okio)
            api(kotlinx.serialization.protobuf)
            api(kotlinx.immutables)
            api("androidx.datastore:datastore-preferences-core:1.2.1")
            api("app.cash.sqldelight:runtime:2.4.1")
            api(libs.sqldelight.coroutines.extensions)
            api("app.cash.sqldelight:androidx-paging3-extensions:2.4.1")
            api(libs.logcat)

            // Network and scraping, what the extensions get from the app
            api(libs.okhttp)
            api(libs.okhttp.logging.interceptor)
            api(libs.okhttp.dnsoverhttps)
            api(libs.okhttp.brotli)
            api(libs.okio)
            api(libs.jsoup)
            api(libs.rxjava3)
            api(libs.injekt)
            api(libs.jsunpacker)
            api("org.jspecify:jspecify:1.0.0")
        }
        androidMain.dependencies {
            api(androidx.datastore)
            api(libs.sqldelight.android.driver)
            api(kotlinx.coroutines.android)
        }
        val desktopMain by getting {
            dependencies {
                api("app.cash.sqldelight:sqlite-driver:2.4.1")
                api("org.jetbrains.kotlinx:kotlinx-coroutines-swing")
                // Android's built-in org.json
                api("org.json:json:20260814")
            }
        }
    }
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        // The extensions call the extension API's interface methods as the Android app compiles them
        jvmDefault.set(JvmDefaultMode.ENABLE)
    }
}

sqldelight {
    databases {
        create("Database") {
            packageName.set("com.sf.tadami")
            dialect(libs.sqldelight.dialects.sql)
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "com.sf.tadami.shared.resources"
}
