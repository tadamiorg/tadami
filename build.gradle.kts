import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.BasePlugin

buildscript {
    repositories {
        google()
        mavenCentral()
    }
}

plugins {
    id ("com.android.application") version "7.4.1" apply false
    id ("com.android.library") version "7.4.1" apply false
    id ("org.jetbrains.kotlin.android") version "1.8.10" apply false
    id ("org.jetbrains.kotlin.plugin.serialization") version "1.8.10" apply false
}

subprojects {
    tasks.withType<KotlinJvmCompile> {
        kotlinOptions {
            jvmTarget = JavaVersion.VERSION_1_8.toString()
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
            compileSdkVersion(33)
            defaultConfig {
                minSdk = 28
                targetSdk = 33
            }

            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_1_8
                targetCompatibility = JavaVersion.VERSION_1_8
            }
        }
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.buildDir)
}


