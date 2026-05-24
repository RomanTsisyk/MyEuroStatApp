plugins {
    id("eurostat.kmp.library")
    alias(libs.plugins.kotlin.serialization)
}

android.namespace = "eu.eurostat.core.network"

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.coreCommon)
            implementation(projects.coreJsonstat)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.koin.core)
        }
        androidMain.dependencies { implementation(libs.ktor.client.okhttp) }
        iosMain.dependencies { implementation(libs.ktor.client.darwin) }
        val desktopMain by getting {
            dependencies { implementation(libs.ktor.client.okhttp) }
        }
        commonTest.dependencies {
            implementation(libs.ktor.client.mock)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
