import com.android.build.api.artifact.SingleArtifact
import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.nio.ByteBuffer
import java.security.MessageDigest

// An extension built against extensions-lib 1.7 as a lib 1.7 extension is: compileOnly, implementing
// only the suspend methods, with a source ID generated like tadami-extensions-source does. :app's
// instrumented tests load its debug APK (ExtensionsLibSampleTest). Never published.
plugins {
    id("com.android.application")
}

val pkgName = "com.sf.tadami.extension.en.libsample"
val libVersion = providers.fileContents(isolated.rootProject.projectDirectory.file("extensions-lib/gradle.properties"))
    .asText.get()
    .lines()
    .first { it.startsWith("extensionsLibVersion=") }
    .substringAfter('=')
    .trim()
    .split('.')
    .take(2)
    .joinToString(".")

// tadami-extensions-source's formula (source-ids.gradle): the first 8 bytes, big-endian, of the MD5 of
// "tadami-source/v1/<repoId>/<package>/<source key>/<versionId>", without the sign bit
fun sourceId(repoId: String, pkgName: String, sourceKey: String, versionId: Int): Long {
    val md5 = MessageDigest.getInstance("MD5").digest("tadami-source/v1/$repoId/$pkgName/$sourceKey/$versionId".toByteArray())
    return ByteBuffer.wrap(md5, 0, 8).long and Long.MAX_VALUE
}

android {
    namespace = pkgName

    defaultConfig {
        applicationId = pkgName
        versionCode = 1
        // <lib major.minor>.<code>, what the app reads the lib version from
        versionName = "$libVersion.1"
        buildConfigField("long", "SOURCE_ID", "${sourceId("tadamiorg/tadami", pkgName, "LibSample", 1)}L")
    }

    buildFeatures {
        buildConfig = true
    }
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmDefault.set(JvmDefaultMode.ENABLE)
    }
}

dependencies {
    compileOnly(project(":extensions-lib"))
}

// Like the published extensions, it doesn't ship the Kotlin standard library: the app provides it
configurations.matching { it.name.endsWith("RuntimeClasspath") }.configureEach {
    exclude(group = "org.jetbrains.kotlin")
    exclude(group = "org.jetbrains", module = "annotations")
}

// Its debug APK, for :app's instrumented tests
val sampleApk = tasks.register<Sync>("sampleApk") {
    into(layout.buildDirectory.dir("sampleApk"))
    rename { "lib-sample.apk" }
}

androidComponents {
    onVariants(selector().withBuildType("debug")) { variant ->
        val apkDir = variant.artifacts.get(SingleArtifact.APK)
        sampleApk.configure {
            from(apkDir) { include("*.apk") }
        }
    }
}

configurations.consumable("sampleApkElements") {
    outgoing.artifact(sampleApk.map { it.destinationDir }) {
        builtBy(sampleApk)
    }
}
