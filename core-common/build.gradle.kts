plugins { id("eurostat.kmp.library") }

android.namespace = "eu.eurostat.core.common"

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.koin.core)
        }
    }
}
