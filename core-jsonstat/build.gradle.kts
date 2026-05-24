plugins {
    id("eurostat.kmp.library")
    alias(libs.plugins.kotlin.serialization)
}

android.namespace = "eu.eurostat.core.jsonstat"

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
        }
    }
}
