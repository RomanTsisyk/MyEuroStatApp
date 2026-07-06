plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}

val libs = the<org.gradle.accessors.dm.LibrariesForLibs>()

kotlin {
    jvmToolchain(17)
    androidTarget()
    jvm("desktop")
    wasmJs {
        browser()
        binaries.executable()
    }
    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    applyDefaultHierarchyTemplate()

    // See eurostat.kmp.library: exclude macOS AppleDouble (._*) files so the
    // Kotlin compiler never tries to parse them as source on exFAT/FAT volumes.
    sourceSets.all {
        kotlin.exclude("**/._*")
    }
}

android {
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
