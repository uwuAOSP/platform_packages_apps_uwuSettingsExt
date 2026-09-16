import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.api.tasks.compile.JavaCompile

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "org.uwuaosp.settingsext"
    compileSdk = 36

    defaultConfig {
        applicationId = "org.uwuaosp.settingsext"
        minSdk = 35
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".dev"
        }
    }

    sourceSets {
        getByName("main") {
            manifest.srcFile("../AndroidManifest.xml")
            java.setSrcDirs(listOf("../src"))
            res.setSrcDirs(listOf("../res"))
        }
        getByName("debug") {
            res.srcDir("../../../../frameworks/base/packages/SettingsLib/SettingsTheme/res")
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        // Java 8 mode allows javac to prepend the platform framework stubs below.
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
            freeCompilerArgs.add("-Xjvm-default=all")
        }
    }
}

dependencies {
    // Generated from matching Soong outputs by pull-system-libs.sh.
    compileOnly(fileTree("../system_libs") { include("*.jar") })

    implementation(project(":uwu-compose"))
    implementation("androidx.activity:activity-compose:1.12.0-alpha08")
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.3.0-alpha02")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.fragment:fragment-ktx:1.8.8")
    implementation("androidx.preference:preference:1.2.1")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.compose.foundation:foundation:1.10.0-alpha01")
    implementation("androidx.compose.material:material-icons-extended:1.7.0-alpha01")
    implementation("androidx.compose.material3:material3:1.5.0-alpha16")
    implementation("androidx.compose.ui:ui:1.10.0-alpha01")
    implementation("androidx.compose.ui:ui-tooling-preview:1.10.0-alpha01")
    implementation("com.google.android.material:material:1.14.0-alpha08")
    implementation("com.airbnb.android:lottie:6.5.2")

    debugImplementation("androidx.compose.ui:ui-tooling:1.10.0-alpha01")
    debugImplementation(fileTree("../system_libs/debug-runtime") { include("*.jar") })
}

tasks.withType<JavaCompile>().configureEach {
    // The public SDK shadows hidden and uwuAOSP-added members on android.* classes.
    options.compilerArgs.add("-Xbootclasspath/p:${file("../system_libs/framework.jar")}")
}

tasks.withType<KotlinCompile>().configureEach {
    libraries.from(file("../system_libs/framework.jar"))
}
