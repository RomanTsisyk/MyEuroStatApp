plugins {
    id("eurostat.kmp.library")
    alias(libs.plugins.kotlin.serialization)
}

android.namespace = "eu.eurostat.core.navigation"

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.coreCommon)
            implementation(libs.decompose.core)
            implementation(libs.essenty.lifecycle)
            implementation(libs.essenty.lifecycle.coroutines)
            implementation(libs.essenty.state.keeper)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.koin.core)
        }
    }
}
