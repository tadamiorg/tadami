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
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)

    // Loads the extension APKs: signature, manifest, dex to JVM bytecode
    implementation("com.android.tools.build:apksig:9.4.1")
    implementation("net.dongliu:apk-parser:2.6.10")
    implementation("de.femtopedia.dex2jar:dex-translator:2.4.38")

    // Player: libmpv drawn inside Compose (LGPL runtime per OS)
    implementation("org.openani.mediamp:mediamp-all:0.5.3")
    runtimeOnly("org.openani.mediamp:mediamp-mpv-runtime-windows-x64:0.5.3")
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
