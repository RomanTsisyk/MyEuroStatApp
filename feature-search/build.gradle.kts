plugins { id("eurostat.kmp.compose") }

android.namespace = "eu.eurostat.feature.search"

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.coreNavigation)
            implementation(projects.coreUi)
            implementation(libs.decompose.core)
        }
    }
}
