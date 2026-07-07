plugins { id("eurostat.kmp.sqldelight") }

android.namespace = "eu.eurostat.core.database"

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.coreCommon)
            implementation(libs.koin.core)
        }
        val desktopTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.sqldelight.sqlite.driver)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }
}

