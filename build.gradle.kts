import com.android.build.gradle.BaseExtension
import com.android.build.gradle.BasePlugin
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import kotlin.collections.addAll

buildscript {
    dependencies {
        classpath(libs.sqldelight.gradle)
    }
    repositories {
        google()
        mavenCentral()
    }
}

plugins {
    alias(androidx.plugins.com.android.application) apply false
    alias(androidx.plugins.com.android.library) apply false
    alias(kotlinx.plugins.compose.compiler) apply false
    alias(kotlinx.plugins.android) apply false
    alias(kotlinx.plugins.serialization) apply false
    alias(libs.plugins.dotenv)
}

subprojects {
    tasks.withType<KotlinJvmCompile> {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        testLogging {
            showCauses = true
            showExceptions = true
            showStackTraces = true
        }
    }

    plugins.withType<BasePlugin> {
        configure<BaseExtension> {
            compileSdkVersion(36)
            defaultConfig {
                minSdk = 28
                targetSdk = 36
            }

            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
        }
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}