plugins { id("eurostat.kmp.compose") }

android.namespace = "eu.eurostat.core.ui"

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(compose.materialIconsExtended)
            implementation(projects.coreCommon)
        }
    }
}
