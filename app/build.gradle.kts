import java.text.SimpleDateFormat
import java.util.Date
import java.util.Properties
import java.util.TimeZone

plugins {
    id("com.android.application")
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

// Release keystore, from environment variables (CI) or the local .env. Read through fetchOrNull: the
// generated env.X accessors only exist for keys present in .env, so a checkout without it (forks, CI)
// would not even configure. TADAMI_KEYSTORE_PATH is the CI name, SIGNING_KEY the legacy local .env one.
val releaseKeystorePath = listOf("TADAMI_KEYSTORE_PATH", "SIGNING_KEY")
    .firstNotNullOfOrNull { key -> env.fetchOrNull(key)?.takeIf { it.isNotBlank() } }
val releaseKeystore = releaseKeystorePath?.let { file(it) }?.takeIf { it.exists() }
if (releaseKeystorePath != null && releaseKeystore == null) {
    logger.warn("Release keystore '$releaseKeystorePath' not found: release builds will be unsigned")
}

// The release workflow passes the version it computed from the commits since the last tag
// (-PappVersionName=1.10.0, see docs/RELEASING.md). Other builds take the latest vX.Y.Z tag, or 0.0.1
// when the tags weren't fetched (CI's shallow clone).
val appVersionName = providers.gradleProperty("appVersionName").orNull ?: latestReleaseVersion() ?: "0.0.1"

android {
    namespace = "com.sf.tadami"
    buildFeatures.buildConfig = true

    defaultConfig {
        applicationId = "com.sf.tadami"
        versionCode = versionCodeOf(appVersionName)
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "BUILD_DATE", "\"${getBuildDate()}\"")
        // Version name without the debug suffix: the GitHub release tag is v<RELEASE_VERSION_NAME>
        buildConfigField("String", "RELEASE_VERSION_NAME", "\"$versionName\"")
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = releaseKeystore
                storePassword = env.fetchOrNull("KEY_STORE_PASSWORD")
                keyAlias = env.fetchOrNull("ALIAS")
                keyPassword = env.fetchOrNull("KEY_PASSWORD")?.takeIf { it.isNotEmpty() } ?: storePassword
            }
        }
    }

    buildTypes {
        named("debug"){
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
            isDebuggable = true
            // The maintainer's debug builds are signed with the release keystore when it is available
            // locally (keeps existing debug installs updatable); forks and CI use AGP's debug keystore.
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
            proguardFiles("proguard-android-optimize.txt", "proguard-rules.pro")
            buildConfigField("boolean", "UPDATER_ENABLED", "false")
        }
        named("release") {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            // Unsigned without a keystore, so CI can still run assembleRelease (R8, lintVital) on PRs
            signingConfig = signingConfigs.findByName("release")
            proguardFiles("proguard-android-optimize.txt", "proguard-rules.pro")
            buildConfigField("boolean", "UPDATER_ENABLED", "true")
        }
    }
    buildFeatures {
        compose = true
        viewBinding = true
    }

    // JVM tests of classes that log (e.g. the cast proxy): android.util stubs return defaults
    testOptions.unitTests.isReturnDefaultValues = true

    packaging {
        resources {
            excludes.add("/META-INF/{AL2.0,LGPL2.1}")
            excludes.add("META-INF/INDEX.LIST")
            excludes.add("META-INF/io.netty.versions.properties")
        }
    }

}

androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            output.outputFileName.set("tadami.apk")
        }
    }
}

// ExtensionsApiCompatibilityTest checks the app still provides what extensions link against: the
// classes of :extensions-lib, the stub they compile against, and those of the released lib versions,
// frozen in extensions-lib/api. -Ptadami.updateApiBaseline=X.Y adds the stub to extensions-lib/api/X.Y.txt
// (when tagging lib-X.Y.Z, see extensions-lib/README.md).
val extensionsApiClasses = configurations.dependencyScope("extensionsApiClasses")
val extensionsApiClassesFiles = configurations.resolvable("extensionsApiClassesFiles") {
    extendsFrom(extensionsApiClasses.get())
}
val extensionsLibDir = isolated.rootProject.projectDirectory.dir("extensions-lib")
val extensionsLibVersion = providers.fileContents(extensionsLibDir.file("gradle.properties")).asText.map { text ->
    Properties().apply { load(text.reader()) }.getProperty("extensionsLibVersion")
}

abstract class ExtensionsApiTestArguments : CommandLineArgumentProvider {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val stubClasses: ConfigurableFileCollection

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val baselines: DirectoryProperty

    @get:Input
    abstract val libVersion: Property<String>

    @get:Input
    @get:Optional
    abstract val updateBaseline: Property<String>

    override fun asArguments(): List<String> = listOfNotNull(
        "-Dtadami.extensionsApiClasses=${stubClasses.files.joinToString(File.pathSeparator)}",
        "-Dtadami.extensionsApiBaselines=${baselines.get().asFile.absolutePath}",
        "-Dtadami.extensionsLibVersion=${libVersion.get()}",
        updateBaseline.orNull?.let { "-Dtadami.updateApiBaseline=$it" },
    )
}

tasks.withType<Test>().configureEach {
    val arguments = objects.newInstance<ExtensionsApiTestArguments>().apply {
        stubClasses.from(extensionsApiClassesFiles)
        baselines.set(extensionsLibDir.dir("api"))
        libVersion.set(extensionsLibVersion)
        updateBaseline.set(providers.gradleProperty("tadami.updateApiBaseline"))
    }
    jvmArgumentProviders.add(arguments)
    if (arguments.updateBaseline.isPresent) {
        // Writes extensions-lib/api/X.Y.txt: never skipped nor taken from the build cache
        outputs.upToDateWhen { false }
        outputs.cacheIf { false }
    }
}

// 1.10.0 gives 1010000: it grows with the version, and stays above 52, the last hand-set code (1.9.3)
fun versionCodeOf(versionName: String): Int {
    val parts = Regex("""(\d+)\.(\d+)\.(\d+)""").matchEntire(versionName)?.groupValues?.drop(1)?.map { it.toInt() }
    require(parts != null && parts[1] < 1000 && parts[2] < 1000) {
        "Version '$versionName' isn't X.Y.Z with Y and Z below 1000"
    }
    val (major, minor, patch) = parts
    return major * 1_000_000 + minor * 1_000 + patch
}

// Highest vX.Y.Z tag, so local builds carry the version of the last release. Null without git or tags.
fun latestReleaseVersion(): String? = runCatching {
    providers.exec {
        commandLine("git", "tag", "--list", "v*")
    }.standardOutput.asText.get()
}.getOrNull()
    ?.lines()
    ?.map { it.trim().removePrefix("v") }
    ?.filter { Regex("""\d+\.\d+\.\d+""").matches(it) }
    ?.maxByOrNull { versionCodeOf(it) }

// Last commit time instead of the wall clock, so BuildConfig (and everything compiled against it) stays
// up to date and cacheable for a given commit. Falls back to a constant when git is unavailable.
fun getBuildDate(): String {
    val commitEpochSeconds = runCatching {
        providers.exec {
            commandLine("git", "log", "-1", "--format=%ct")
        }.standardOutput.asText.get().trim().toLong()
    }.getOrDefault(0L)
    val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
    format.timeZone = TimeZone.getTimeZone("UTC")
    return format.format(Date(commitEpochSeconds * 1000))
}

dependencies {

    implementation(androidx.core.ktx)


    androidTestImplementation(androidx.androidx.test.ext.junit)
    androidTestImplementation(androidx.androidx.test.runner)
    androidTestImplementation(androidx.androidx.test.rules)
    androidTestImplementation(androidx.espresso.core)
    androidTestImplementation(libs.okhttp.mockwebserver3)

    // Kotlin Coroutines
    implementation(platform(kotlinx.coroutines.bom))
    implementation(kotlinx.bundles.coroutines)
    implementation(androidx.material)

    // Coil
    implementation(platform(libs.coil.bom))
    implementation(libs.bundles.coilGroup)

    // Exoplayer
    implementation(androidx.bundles.exoplayerGroup)

    // Compose
    implementation(platform(compose.bom))
    implementation(compose.activity)
    implementation(compose.bundles.animationGroup)
    implementation(compose.bundles.uiGroup)
    implementation(compose.bundles.runtimeGroup)
    implementation(compose.bundles.material)
    implementation(libs.compose.webview)
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
    implementation(libs.bundles.okhttpGroup)

    // Local HTTP proxy for the web cast receiver
    implementation(libs.nanohttpd)

    testImplementation(libs.junit)
    testImplementation(libs.okhttp.mockwebserver3)
    testImplementation(libs.asm)
    testImplementation(libs.asm.tree)
    testImplementation(kotlinx.metadata.jvm)
    extensionsApiClasses(project(":extensions-lib", "apiClassesElements"))

    // SQLite
    implementation(libs.bundles.sqlite)

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

    // JSUnpacker
    implementation(libs.jsunpacker)
    implementation(libs.unifile)

    // Logcat
    implementation(libs.logcat)

}
tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions {
            // Interface methods with a body become JVM default methods, plus DefaultImpls for the
            // extensions compiled without them. Pinned like in :extensions-lib: changing it breaks them.
            jvmDefault.set(org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode.ENABLE)
            freeCompilerArgs.addAll(
                "-Xcontext-parameters",
                "-XXLanguage:+PropertyParamAnnotationDefaultTargetMode"
            )
        }
    }
}