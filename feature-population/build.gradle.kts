plugins {
    id("eurostat.kmp.compose")
    alias(libs.plugins.kotlin.serialization)
}

android.namespace = "eu.eurostat.feature.population"

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.coreCommon)
            implementation(projects.coreJsonstat)
            implementation(projects.coreNetwork)
            implementation(projects.coreDatabase)
            implementation(projects.coreNavigation)
            implementation(projects.coreUi)
            implementation(projects.coreCharts)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.decompose.core)
            implementation(libs.essenty.lifecycle.coroutines)
            implementation(libs.koin.core)
        }
        commonTest.dependencies {
            implementation(libs.turbine)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.kotlinx.serialization.json)
        }
    }
}
