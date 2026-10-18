pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(url = "https://www.jitpack.io")
    }
    versionCatalogs {
        create("kotlinx") {
            from(files("gradle/kotlinx.versions.toml"))
        }
        create("androidx") {
            from(files("gradle/androidx.versions.toml"))
        }
        create("compose") {
            from(files("gradle/compose.versions.toml"))
        }
    }
}
rootProject.name = "Tadami"
// JitPack only builds and publishes extensions-lib (see jitpack.yml)
if (System.getenv("JITPACK") != "true") {
    include(":app")
    // An extension built against the lib, for :app's instrumented tests
    include(":extensions-lib:sample")
}
include(":extensions-lib")
