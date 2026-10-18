import com.android.build.api.artifact.ScopedArtifact
import com.android.build.api.variant.ScopedArtifacts
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
