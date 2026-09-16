import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "org.uwuaosp.compose"
    compileSdk = 36

    defaultConfig {
        minSdk = 35
    }

    sourceSets {
        getByName("main") {
            manifest.srcFile("../../../../../uwu-sdk/uwuCompose/AndroidManifest.xml")
            java.setSrcDirs(listOf("../../../../../uwu-sdk/uwuCompose/src"))
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
            freeCompilerArgs.add("-Xjvm-default=all")
        }
    }
}

dependencies {
    implementation("androidx.compose.foundation:foundation:1.10.0-alpha01")
    implementation("androidx.compose.material:material-icons-extended:1.7.0-alpha01")
    implementation("androidx.compose.material3:material3:1.5.0-alpha16")
    implementation("androidx.compose.ui:ui:1.10.0-alpha01")
    implementation("androidx.compose.ui:ui-graphics:1.10.0-alpha01")
}
