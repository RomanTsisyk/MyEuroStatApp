plugins { id("eurostat.kmp.compose") }

android.namespace = "eu.eurostat.core.charts"

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.coreCommon)
        }
    }
}
