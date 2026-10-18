import com.android.build.api.artifact.ScopedArtifact
import com.android.build.api.variant.ScopedArtifacts
import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.android.library")
    kotlin("plugin.serialization")
    `maven-publish`
}

android {
    namespace = "com.sf.tadami.extensions.lib"

    buildFeatures {
        buildConfig = false
        androidResources = false
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
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

// Published on JitPack from the tag lib-<extensionsLibVersion>, which JitPack passes in VERSION:
// com.github.tadamiorg:tadami:lib-X.Y.Z (see README.md)
val extensionsLibVersion = property("extensionsLibVersion") as String
val onJitPack = System.getenv("JITPACK") == "true"
val publishedVersion = System.getenv("VERSION")?.takeIf { onJitPack } ?: "lib-$extensionsLibVersion"
// JitPack builds any tag or commit asked for: a lib-* tag must be on a commit of that lib version
if (onJitPack && publishedVersion.startsWith("lib-")) {
    check(publishedVersion == "lib-$extensionsLibVersion") {
        "Tag $publishedVersion, but extensions-lib/gradle.properties has extensionsLibVersion=$extensionsLibVersion"
    }
}

publishing {
    publications {
        register<MavenPublication>("release") {
            // JitPack serves the only module of a build as com.github.<owner>:<repo>: the same here,
            // so a lib published to mavenLocal replaces the released one
            groupId = "com.github.tadamiorg"
            artifactId = "tadami"
            version = publishedVersion
            afterEvaluate {
                from(components["release"])
            }
            pom {
                name.set("Tadami extensions-lib")
                description.set("The API Tadami extensions compile against (compileOnly), provided by the app at runtime")
                url.set("https://github.com/tadamiorg/tadami")
            }
        }
    }
}

// The compiled stub, for :app's ExtensionsApiCompatibilityTest (and its release keep rules)
abstract class ExportApiClasses : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val jars: ListProperty<RegularFile>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val dirs: ListProperty<Directory>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Inject
    abstract val files: FileSystemOperations

    @get:Inject
    abstract val archives: ArchiveOperations

    @TaskAction
    fun export() {
        files.sync {
            into(outputDir)
            dirs.get().forEach { from(it) }
            jars.get().forEach { from(archives.zipTree(it)) }
            include("**/*.class")
        }
    }
}

val exportApiClasses = tasks.register<ExportApiClasses>("exportApiClasses") {
    outputDir.set(layout.buildDirectory.dir("apiClasses"))
}

androidComponents {
    onVariants(selector().withBuildType("release")) { variant ->
        variant.artifacts.forScope(ScopedArtifacts.Scope.PROJECT)
            .use(exportApiClasses)
            .toGet(ScopedArtifact.CLASSES, ExportApiClasses::jars, ExportApiClasses::dirs)
    }
}

configurations.consumable("apiClassesElements") {
    outgoing.artifact(exportApiClasses.flatMap { it.outputDir })
}
