plugins { id("eurostat.kmp.compose") }

android.namespace = "eu.eurostat.core.ui"

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(compose.materialIconsExtended)
            implementation(projects.coreCommon)
        }
        // WindowCompat / WindowInsetsControllerCompat for StatusBarIcons. Same
        // catalog entry (and version) composeApp already uses.
        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
        }
    }
}
