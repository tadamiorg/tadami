import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.BasePlugin

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
    alias(kotlinx.plugins.android) apply false
    alias(kotlinx.plugins.serialization) apply false
}

subprojects {
    tasks.withType<KotlinJvmCompile> {
        kotlinOptions {
            jvmTarget = JavaVersion.VERSION_17.toString()
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
            compileSdkVersion(34)
            defaultConfig {
                minSdk = 28
                targetSdk = 33
            }

            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
        }
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.buildDir)
}


