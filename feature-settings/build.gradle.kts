plugins { id("eurostat.kmp.compose") }

android.namespace = "eu.eurostat.feature.settings"

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.coreCommon)
            implementation(projects.coreNavigation)
            implementation(projects.coreUi)
            implementation(libs.decompose.core)
            implementation(libs.essenty.lifecycle.coroutines)
            implementation(libs.koin.core)
        }
    }
}
