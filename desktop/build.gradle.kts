import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    alias(kotlinx.plugins.compose.compiler)
    alias(kotlinx.plugins.compose.multiplatform)
    kotlin("plugin.serialization")
}

kotlin {
    jvmToolchain(17)
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        // The extensions call the extension API's interface methods as the Android app compiles them
        jvmDefault.set(JvmDefaultMode.ENABLE)
    }
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.compose.material3:material3:1.9.0")
    implementation("org.jetbrains.compose.material:material-icons-extended:1.7.3")

    // What the extensions compile against (compileOnly): the app provides it at runtime
    implementation(platform(kotlinx.coroutines.bom))
    implementation(kotlinx.coroutines.core)
    implementation(kotlinx.coroutines.rx3)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing")
    implementation(kotlinx.serialization.json)
    implementation(kotlinx.serialization.json.okio)
    implementation(kotlinx.serialization.protobuf)
    implementation(kotlinx.immutables)
    implementation(libs.okhttp)
    implementation(libs.okhttp.brotli)
    implementation(libs.okhttp.dnsoverhttps)
    implementation(libs.okio)
    implementation(libs.jsoup)
    implementation(libs.rxjava3)
    implementation(libs.injekt)
    implementation("org.jspecify:jspecify:1.0.0")
    implementation("androidx.datastore:datastore-preferences-core:1.2.1")

    // Loads the extension APKs: signature, manifest, dex to JVM bytecode
    implementation("com.android.tools.build:apksig:9.4.1")
    implementation("net.dongliu:apk-parser:2.6.10")
    implementation("de.femtopedia.dex2jar:dex-translator:2.4.38")

    // Player: libmpv drawn inside Compose (LGPL runtime per OS)
    implementation("org.openani.mediamp:mediamp-all:0.5.3")
    runtimeOnly("org.openani.mediamp:mediamp-mpv-runtime-windows-x64:0.5.3")

    // Images
    implementation("io.coil-kt.coil3:coil-compose:3.6.3")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.6.3")
}

compose.desktop {
    application {
        mainClass = "com.sf.tadami.desktop.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe, TargetFormat.Dmg, TargetFormat.Deb)
            packageName = "Tadami"
            packageVersion = "1.0.0"
            modules("java.sql", "jdk.unsupported", "jdk.localedata", "jdk.charsets", "jdk.zipfs")
        }
    }
}

// Installs the official extensions and runs each source end to end, without the UI: gradlew :desktop:smoke [-Psmoke.only=animesama]
tasks.register<JavaExec>("smoke") {
    group = "verification"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.sf.tadami.desktop.SmokeKt")
    (findProperty("smoke.only") as String?)?.let { args(it) }
}
