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

// The mpv runtime of the OS the app runs or is packaged on: each OS builds its own installer
val mpvRuntime = run {
    val os = System.getProperty("os.name").lowercase()
    val arm = System.getProperty("os.arch") in setOf("aarch64", "arm64")
    when {
        "win" in os -> if (arm) "windows-arm64" else "windows-x64"
        "mac" in os -> if (arm) "macos-arm64" else "macos-x64"
        else -> "linux-x64"
    }
}

// Chromium for the WebView and the Cloudflare challenges, bundled for the same OS as mpv
val jcefVersion = "152.0.6"
val jcefNatives = "jcef-17e805a+cef-152.0.6+g708dc14+chromium-152.0.7977.83"
val jcefPlatform = when (mpvRuntime) {
    "windows-x64" -> "windows-amd64"
    "windows-arm64" -> "windows-arm64"
    "macos-x64" -> "macosx-amd64"
    "macos-arm64" -> "macosx-arm64"
    else -> "linux-amd64"
}
// JCEF reaches the native window handles of AWT
val jcefJvmArgs = when {
    mpvRuntime.startsWith("macos") -> listOf("sun.awt", "sun.lwawt", "sun.lwawt.macosx")
    mpvRuntime.startsWith("windows") -> listOf("sun.awt", "sun.awt.windows")
    else -> listOf("sun.awt", "sun.awt.X11")
}.map { "--add-opens=java.desktop/$it=ALL-UNNAMED" }

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)

    // Loads the extension APKs: signature, manifest, dex to JVM bytecode
    implementation("com.android.tools.build:apksig:9.4.1")
    implementation("net.dongliu:apk-parser:2.6.10")
    implementation("de.femtopedia.dex2jar:dex-translator:2.4.38")

    // Player: libmpv drawn inside Compose (LGPL runtime per OS)
    implementation("org.openani.mediamp:mediamp-all:0.5.3")
    runtimeOnly("org.openani.mediamp:mediamp-mpv-runtime-$mpvRuntime:0.5.3")

    implementation("me.friwi:jcefmaven:$jcefVersion")
    runtimeOnly("me.friwi:jcef-natives-$jcefPlatform:$jcefNatives")
}

compose.desktop {
    application {
        mainClass = "com.sf.tadami.desktop.MainKt"
        jvmArgs += jcefJvmArgs
        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe, TargetFormat.Dmg, TargetFormat.Deb)
            packageName = "Tadami"
            packageVersion = "1.0.0"
            modules("java.sql", "jdk.unsupported", "jdk.localedata", "jdk.charsets", "jdk.zipfs")
        }
    }
}
